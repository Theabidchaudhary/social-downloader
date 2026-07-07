package app.siphon.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.siphon.SiphonApp
import app.siphon.data.db.DownloadStatus
import app.siphon.util.StorageSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Foreground service that drains the download queue.
 *
 * Lifecycle: any enqueue/resume/retry calls [start], which (re)kicks the
 * dispatch loop. The loop keeps up to `maxParallelDownloads` jobs running,
 * pulls the next QUEUED row when a slot frees up, and stops the service when
 * the queue is empty and nothing is running.
 *
 * Pause vs cancel: both cancel the job's coroutine; an entry in [pauseRequested]
 * decides whether the row ends up PAUSED (partial output kept for range
 * resume) or CANCELED (partial output deleted).
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<Long, Job>()
    private val pauseRequested = mutableSetOf<Long>()
    private val stateLock = Mutex()

    private lateinit var notifications: DownloadNotifications
    private lateinit var downloader: Downloader

    override fun onCreate() {
        super.onCreate()
        val container = (application as SiphonApp).container
        notifications = DownloadNotifications(this)
        downloader = Downloader(
            context = this,
            dao = container.database.downloadDao(),
            repository = container.downloadRepository,
            client = container.okHttpClient,
        )

        // Rows left RUNNING by a killed process go back to the queue.
        scope.launch { container.database.downloadDao().requeueOrphanedRunning() }

        // Progress fan-out: one observer drives all per-download notifications.
        scope.launch {
            val settings = container.settings
            container.database.downloadDao().observeActive().collect { active ->
                if (settings.current().notificationsEnabled) {
                    active.filter { it.status == DownloadStatus.RUNNING }
                        .forEach { notifications.progress(it) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground(activeCount = jobs.size.coerceAtLeast(1))
        when (intent?.action) {
            ACTION_PAUSE -> intent.entityId()?.let { id -> scope.launch { stopJob(id, pause = true) } }
            ACTION_CANCEL -> intent.entityId()?.let { id -> scope.launch { stopJob(id, pause = false) } }
            else -> Unit // ACTION_KICK / restart — just dispatch
        }
        scope.launch { dispatch() }
        return START_STICKY
    }

    /** Fill free slots with queued work; stop the service when idle. */
    private suspend fun dispatch() {
        val container = (application as SiphonApp).container
        val dao = container.database.downloadDao()

        stateLock.withLock {
            val maxParallel = container.settings.current().maxParallelDownloads
            while (jobs.size < maxParallel) {
                val next = dao.nextQueued() ?: break
                dao.setStatus(next.id, DownloadStatus.RUNNING)
                jobs[next.id] = scope.launch { runJob(next.id) }
            }
        }

        stateLock.withLock {
            if (jobs.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                goForeground(jobs.size)
            }
        }
    }

    private suspend fun runJob(id: Long) {
        val container = (application as SiphonApp).container
        val dao = container.database.downloadDao()
        val notifyEnabled = container.settings.current().notificationsEnabled

        try {
            downloader.execute(id)
            dao.byId(id)?.let { done ->
                dao.update(done.copy(status = DownloadStatus.COMPLETED, completedAt = System.currentTimeMillis(), error = null))
                if (notifyEnabled) notifications.completed(done) else notifications.dismiss(id)
            }
        } catch (e: CancellationException) {
            val paused = stateLock.withLock { pauseRequested.remove(id) }
            val entity = dao.byId(id)
            if (paused) {
                dao.setStatus(id, DownloadStatus.PAUSED)
            } else {
                entity?.outputUri?.let { runCatching { StorageSink.fromExisting(this, it).delete() } }
                dao.byId(id)?.let { dao.update(it.copy(status = DownloadStatus.CANCELED, downloadedBytes = 0, outputUri = null)) }
            }
            notifications.dismiss(id)
        } catch (e: Exception) {
            dao.byId(id)?.let { failed ->
                dao.update(failed.copy(status = DownloadStatus.FAILED, error = e.message ?: e.javaClass.simpleName))
                if (notifyEnabled) notifications.failed(failed)
            }
        } finally {
            stateLock.withLock { jobs.remove(id) }
            dispatch()
        }
    }

    private suspend fun stopJob(id: Long, pause: Boolean) {
        val job = stateLock.withLock {
            if (pause) pauseRequested.add(id)
            jobs[id]
        }
        if (job != null) {
            job.cancel()
        } else if (!pause) {
            // Not running (still queued) — flip the row directly.
            val dao = (application as SiphonApp).container.database.downloadDao()
            val entity = dao.byId(id)
            if (entity != null && entity.status == DownloadStatus.QUEUED) {
                dao.setStatus(id, DownloadStatus.CANCELED)
            }
        } else {
            val dao = (application as SiphonApp).container.database.downloadDao()
            val entity = dao.byId(id)
            if (entity != null && entity.status == DownloadStatus.QUEUED) {
                dao.setStatus(id, DownloadStatus.PAUSED)
            }
        }
    }

    private fun goForeground(activeCount: Int) {
        val notification = notifications.foregroundNotification(activeCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                DownloadNotifications.FOREGROUND_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(DownloadNotifications.FOREGROUND_ID, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun Intent.entityId(): Long? = getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }

    companion object {
        private const val ACTION_KICK = "app.siphon.action.KICK"
        private const val ACTION_PAUSE = "app.siphon.action.PAUSE"
        private const val ACTION_CANCEL = "app.siphon.action.CANCEL"
        private const val EXTRA_ID = "entity_id"

        fun start(context: Context) = send(context, ACTION_KICK, null)
        fun pause(context: Context, id: Long) = send(context, ACTION_PAUSE, id)
        fun cancel(context: Context, id: Long) = send(context, ACTION_CANCEL, id)

        private fun send(context: Context, action: String, id: Long?) {
            val intent = Intent(context, DownloadService::class.java).setAction(action)
            id?.let { intent.putExtra(EXTRA_ID, it) }
            context.startForegroundService(intent)
        }
    }
}
