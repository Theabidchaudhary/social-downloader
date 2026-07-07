package app.siphon.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import app.siphon.data.settings.TargetDir
import java.io.File
import java.io.OutputStream

/**
 * Abstraction over the three ways a download can be persisted:
 *  - MediaStore collections (Downloads / Movies / Music) on API 29+
 *  - a user-picked SAF document tree ("custom folder", incl. SD cards)
 *  - direct File I/O into public directories on API 26-28
 *
 * A sink can be (re)opened in append mode, which is what makes pause/resume
 * survive process death: we reopen the same output and continue from the
 * byte offset recorded in the database.
 */
class StorageSink private constructor(
    private val context: Context,
    val uri: Uri,
    private val legacyFile: File?,
) {

    fun open(append: Boolean): OutputStream {
        legacyFile?.let { file ->
            return java.io.FileOutputStream(file, append)
        }
        val mode = if (append) "wa" else "wt"
        return context.contentResolver.openOutputStream(uri, mode)
            ?: throw java.io.IOException("Cannot open output stream for $uri")
    }

    /** Size currently persisted — used to sanity-check resume offsets. */
    fun currentSize(): Long {
        legacyFile?.let { return it.length() }
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    /** Mark a MediaStore item as finished so it becomes visible to other apps. */
    fun finalizeFile() {
        if (legacyFile != null) {
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(legacyFile.absolutePath), null, null,
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.authority == MediaStore.AUTHORITY) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            runCatching { context.contentResolver.update(uri, values, null, null) }
        }
    }

    fun delete() {
        legacyFile?.let { it.delete(); return }
        runCatching { context.contentResolver.delete(uri, null, null) }
            .recoverCatching { DocumentFile.fromSingleUri(context, uri)?.delete() }
    }

    companion object {

        /** Re-attach to an output created earlier (resume path). */
        fun fromExisting(context: Context, outputUri: String): StorageSink {
            val uri = Uri.parse(outputUri)
            return if (uri.scheme == "file") {
                StorageSink(context, uri, File(uri.path!!))
            } else {
                StorageSink(context, uri, null)
            }
        }

        /**
         * Create the output target for a new download and return the sink.
         * File name collisions are resolved by the platform (MediaStore) or
         * by suffixing (SAF/legacy).
         */
        fun create(
            context: Context,
            targetDir: TargetDir,
            customTreeUri: String?,
            fileName: String,
            mimeType: String,
        ): StorageSink {
            if (targetDir == TargetDir.CUSTOM && customTreeUri != null) {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(customTreeUri))
                    ?: throw java.io.IOException("Custom folder is no longer accessible")
                val unique = uniqueName(fileName) { candidate -> tree.findFile(candidate) != null }
                val doc = tree.createFile(mimeType, unique)
                    ?: throw java.io.IOException("Cannot create file in custom folder")
                return StorageSink(context, doc.uri, null)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = when (targetDir) {
                    TargetDir.MOVIES -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    TargetDir.MUSIC -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                val relativePath = when (targetDir) {
                    TargetDir.MOVIES -> Environment.DIRECTORY_MOVIES + "/Siphon"
                    TargetDir.MUSIC -> Environment.DIRECTORY_MUSIC + "/Siphon"
                    else -> Environment.DIRECTORY_DOWNLOADS + "/Siphon"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(collection, values)
                    ?: throw java.io.IOException("MediaStore rejected the file")
                return StorageSink(context, uri, null)
            }

            // API 26-28: classic public directories (WRITE_EXTERNAL_STORAGE granted at runtime).
            @Suppress("DEPRECATION")
            val baseDir = when (targetDir) {
                TargetDir.MOVIES -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                TargetDir.MUSIC -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            val dir = File(baseDir, "Siphon").apply { mkdirs() }
            var file = File(dir, fileName)
            var counter = 1
            while (file.exists()) {
                file = File(dir, numberedName(fileName, counter++))
            }
            file.createNewFile()
            return StorageSink(context, Uri.fromFile(file), file)
        }

        fun mimeTypeFor(container: String): String = when (container) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            else -> "video/mp4"
        }

        private fun uniqueName(fileName: String, exists: (String) -> Boolean): String {
            if (!exists(fileName)) return fileName
            var counter = 1
            while (true) {
                val candidate = numberedName(fileName, counter++)
                if (!exists(candidate)) return candidate
            }
        }

        private fun numberedName(fileName: String, counter: Int): String {
            val dot = fileName.lastIndexOf('.')
            return if (dot > 0) {
                "${fileName.substring(0, dot)} ($counter)${fileName.substring(dot)}"
            } else {
                "$fileName ($counter)"
            }
        }
    }
}
