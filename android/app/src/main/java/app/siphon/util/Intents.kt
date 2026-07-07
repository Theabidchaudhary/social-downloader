package app.siphon.util

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import app.siphon.data.db.DownloadEntity
import app.siphon.data.settings.TargetDir

/** Small helpers for handing completed files off to the rest of the system. */
object Intents {

    fun openFile(context: Context, entity: DownloadEntity) {
        val uri = entity.outputUri?.let(Uri::parse) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, StorageSink.mimeTypeFor(entity.container))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFolder(context: Context, entity: DownloadEntity) {
        val targetDir = runCatching { TargetDir.valueOf(entity.targetDir) }.getOrDefault(TargetDir.DOWNLOADS)
        val intent = when (targetDir) {
            TargetDir.CUSTOM -> entity.customTreeUri?.let { tree ->
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(tree), "vnd.android.document/directory")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            else -> Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } ?: return
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Fall back to the system Files app entry point.
            runCatching {
                context.startActivity(
                    Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
