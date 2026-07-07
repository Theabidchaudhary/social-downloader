package app.siphon.data.repo

import android.content.Context
import app.siphon.data.db.DownloadDao
import app.siphon.data.db.DownloadEntity
import app.siphon.data.db.DownloadStatus
import app.siphon.data.settings.SiphonSettings
import app.siphon.download.DownloadService
import app.siphon.util.StorageSink
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for everything download-related. UI layers only talk to
 * this class; the foreground [DownloadService] executes what is enqueued here.
 */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val mediaRepository: MediaRepository,
) {

    val all: Flow<List<DownloadEntity>> = dao.observeAll()
    val active: Flow<List<DownloadEntity>> = dao.observeActive()

    suspend fun enqueue(media: ResolvedMedia, format: MediaFormat, settings: SiphonSettings): Long {
        val extension = format.container
        val safeTitle = media.title
            .replace(Regex("[\\\\/:*?\"<>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
            .ifEmpty { "siphon-download" }

        val entity = DownloadEntity(
            sourceUrl = media.sourceUrl,
            platform = media.platform,
            title = media.title,
            thumbnailUrl = media.thumbnailUrl,
            qualityLabel = format.qualityLabel,
            container = format.container,
            kind = format.kind,
            directUrl = format.directUrl,
            apiDownloadUrl = format.downloadUrl,
            supportsResume = format.directUrl != null,
            targetDir = settings.targetDir.name,
            customTreeUri = settings.customTreeUri,
            fileName = "$safeTitle.$extension",
            outputUri = null,
            totalBytes = format.sizeBytes ?: -1L,
        )
        val id = dao.insert(entity)
        DownloadService.start(context)
        return id
    }

    suspend fun pause(id: Long) {
        DownloadService.pause(context, id)
    }

    suspend fun resume(id: Long) {
        val entity = dao.byId(id) ?: return
        if (entity.status == DownloadStatus.PAUSED || entity.status == DownloadStatus.FAILED) {
            dao.setStatus(id, DownloadStatus.QUEUED)
            DownloadService.start(context)
        }
    }

    suspend fun retry(id: Long) {
        val entity = dao.byId(id) ?: return
        if (entity.status == DownloadStatus.FAILED || entity.status == DownloadStatus.CANCELED) {
            // Restart from scratch: stale partial output is discarded because
            // the signed URL (and possibly the CDN URL) may have expired.
            entity.outputUri?.let { runCatching { StorageSink.fromExisting(context, it).delete() } }
            dao.update(
                entity.copy(
                    status = DownloadStatus.QUEUED,
                    downloadedBytes = 0,
                    speedBps = 0,
                    etaSeconds = -1,
                    error = null,
                    outputUri = null,
                ),
            )
            DownloadService.start(context)
        }
    }

    suspend fun cancel(id: Long) {
        DownloadService.cancel(context, id)
    }

    suspend fun delete(id: Long, deleteFile: Boolean) {
        val entity = dao.byId(id) ?: return
        if (entity.status.isActive) DownloadService.cancel(context, id)
        if (deleteFile) {
            entity.outputUri?.let { runCatching { StorageSink.fromExisting(context, it).delete() } }
        }
        dao.delete(id)
    }

    suspend fun clearFinished() = dao.clearFinished()

    /**
     * Signed download URLs expire (default 30 min). Before starting or
     * resuming a stale job the service asks for fresh transport URLs; we
     * re-resolve and match the format the user originally picked.
     */
    suspend fun refreshTransportUrls(entity: DownloadEntity): DownloadEntity {
        val outcome = mediaRepository.resolve(entity.sourceUrl)
        val media = (outcome as? ResolveOutcome.Media)?.media
            ?: throw IllegalStateException("Source is no longer a single media item")
        val match = (media.video + media.audio).firstOrNull {
            it.qualityLabel == entity.qualityLabel && it.container == entity.container
        } ?: (if (entity.kind == "video") media.video.firstOrNull() else media.audio.firstOrNull())
            ?: throw IllegalStateException("No matching format available anymore")

        val updated = entity.copy(
            directUrl = match.directUrl,
            apiDownloadUrl = match.downloadUrl,
            supportsResume = match.directUrl != null,
            totalBytes = if (entity.totalBytes > 0) entity.totalBytes else match.sizeBytes ?: -1L,
        )
        dao.update(updated)
        return updated
    }
}
