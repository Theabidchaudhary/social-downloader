package app.siphon.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED;

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELED
    val isActive: Boolean get() = this == QUEUED || this == RUNNING
}

/**
 * One row per requested download. This is both the queue (QUEUED/RUNNING)
 * and the persistent history (COMPLETED/FAILED/…) — a single table keeps
 * the Downloads screen a trivial reactive query.
 */
@Entity(
    tableName = "downloads",
    indices = [Index("status"), Index("createdAt")],
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // What the user asked for — enough to re-resolve fresh URLs on retry.
    val sourceUrl: String,
    val platform: String,
    val title: String,
    val thumbnailUrl: String?,
    val qualityLabel: String,
    val container: String, // mp4 | mp3 | m4a
    val kind: String, // video | audio

    // Transport. Both URLs expire; the repository refreshes them on demand.
    val directUrl: String?,
    val apiDownloadUrl: String,
    val supportsResume: Boolean,

    // Destination
    val targetDir: String, // TargetDir enum name, or "CUSTOM"
    val customTreeUri: String?, // persisted SAF tree when targetDir == CUSTOM
    val fileName: String,
    val outputUri: String?, // content:// or file:// once writing starts

    // Progress
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val speedBps: Long = 0L,
    val etaSeconds: Long = -1L,
    val error: String? = null,
    val verified: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)
