package app.siphon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.siphon.R
import app.siphon.data.db.DownloadEntity
import app.siphon.data.db.DownloadStatus
import app.siphon.ui.theme.SiphonColors
import app.siphon.util.Formatters
import coil.compose.AsyncImage

/**
 * One download in a list: thumbnail, metadata, live progress and the actions
 * valid for its current status. Used on Home (compact) and Downloads (full).
 */
@Composable
fun DownloadRow(
    entity: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = entity.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(if (compact) 72.dp else 96.dp)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entity.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${entity.qualityLabel} · ${entity.container.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusLine(entity)
                }
            }

            if (entity.status == DownloadStatus.RUNNING || entity.status == DownloadStatus.PAUSED) {
                val progress = Formatters.progressPercent(entity.downloadedBytes, entity.totalBytes)
                if (entity.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraSmall),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraSmall),
                    )
                }
            }

            if (!compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (entity.status) {
                        DownloadStatus.RUNNING -> {
                            ActionIcon(Icons.Filled.Pause, R.string.action_pause, onPause)
                            ActionIcon(Icons.Filled.Cancel, R.string.action_cancel, onCancel)
                        }
                        DownloadStatus.QUEUED -> {
                            ActionIcon(Icons.Filled.Cancel, R.string.action_cancel, onCancel)
                        }
                        DownloadStatus.PAUSED -> {
                            ActionIcon(Icons.Filled.PlayArrow, R.string.action_resume, onResume)
                            ActionIcon(Icons.Filled.Cancel, R.string.action_cancel, onCancel)
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELED -> {
                            ActionIcon(Icons.Filled.Refresh, R.string.action_retry, onRetry)
                            ActionIcon(Icons.Filled.Delete, R.string.action_delete, onDelete)
                        }
                        DownloadStatus.COMPLETED -> {
                            ActionIcon(Icons.Filled.PlayArrow, R.string.action_open_file, onOpenFile)
                            ActionIcon(Icons.Filled.Folder, R.string.action_open_folder, onOpenFolder)
                            ActionIcon(Icons.Filled.Delete, R.string.action_delete, onDelete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(entity: DownloadEntity) {
    val (text, color) = when (entity.status) {
        DownloadStatus.RUNNING ->
            "${Formatters.bytes(entity.downloadedBytes)} / ${Formatters.bytes(entity.totalBytes)} · " +
                "${Formatters.speed(entity.speedBps)}" +
                (if (entity.etaSeconds >= 0) " · ${Formatters.eta(entity.etaSeconds)}" else "") to
                MaterialTheme.colorScheme.primary
        DownloadStatus.QUEUED -> stringResource(R.string.filter_queued) to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.PAUSED -> stringResource(R.string.filter_paused) to SiphonColors.Warning
        DownloadStatus.COMPLETED -> Formatters.bytes(entity.downloadedBytes) to SiphonColors.Success
        DownloadStatus.FAILED -> (entity.error ?: stringResource(R.string.filter_failed)) to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELED -> stringResource(R.string.action_cancel) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = stringResource(labelRes),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
