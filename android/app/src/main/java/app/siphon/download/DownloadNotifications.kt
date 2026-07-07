package app.siphon.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.siphon.MainActivity
import app.siphon.R
import app.siphon.data.db.DownloadEntity
import app.siphon.util.Formatters

/** All notification plumbing for the download manager lives here. */
class DownloadNotifications(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_downloads),
            NotificationManager.IMPORTANCE_LOW, // silent progress updates
        ).apply {
            description = context.getString(R.string.notif_channel_downloads_desc)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_DOWNLOADS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun foregroundNotification(activeCount: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_downloading))
            .setContentText(
                context.resources.getQuantityString(
                    R.plurals.notif_active_count, activeCount, activeCount,
                ),
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()

    fun progress(entity: DownloadEntity) {
        if (!hasPermission()) return
        val percent = Formatters.progressPercent(entity.downloadedBytes, entity.totalBytes)
        val indeterminate = entity.totalBytes <= 0
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(entity.title)
            .setContentText(
                "${Formatters.bytes(entity.downloadedBytes)} · ${Formatters.speed(entity.speedBps)}" +
                    if (entity.etaSeconds >= 0) " · ${Formatters.eta(entity.etaSeconds)}" else "",
            )
            .setProgress(100, percent, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()
        manager.notify(notificationId(entity.id), notification)
    }

    fun completed(entity: DownloadEntity) {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_complete))
            .setContentText(entity.fileName)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        manager.notify(notificationId(entity.id), notification)
    }

    fun failed(entity: DownloadEntity) {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_failed))
            .setContentText(entity.title)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        manager.notify(notificationId(entity.id), notification)
    }

    fun dismiss(id: Long) = manager.cancel(notificationId(id))

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "downloads"
        const val FOREGROUND_ID = 1
        private fun notificationId(entityId: Long): Int = (1000 + entityId).toInt()
    }
}
