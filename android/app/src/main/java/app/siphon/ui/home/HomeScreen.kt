package app.siphon.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import android.widget.Toast
import app.siphon.R
import app.siphon.ui.components.DownloadRow
import app.siphon.ui.components.MediaOptionsContent
import app.siphon.ui.components.PlatformChipsRow
import app.siphon.ui.components.PlaylistContent
import app.siphon.ui.components.ResolveError
import app.siphon.ui.components.ResolveLoading
import app.siphon.ui.components.ResolveUiState
import app.siphon.ui.components.SectionLabel
import app.siphon.util.Intents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val url by viewModel.url.collectAsState()
    val resolveState by viewModel.resolveState.collectAsState()
    val clipboardSuggestion by viewModel.clipboardSuggestion.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    // Clipboard detection on every return to the screen (setting-gated in the VM).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onClipboard(readClipboard(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.home_headline),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.home_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        // URL input + paste + fetch
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = viewModel::onUrlChange,
                placeholder = { Text(stringResource(R.string.url_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                trailingIcon = {
                    IconButton(onClick = { readClipboard(context)?.let(viewModel::onUrlChange) }) {
                        Icon(
                            Icons.Filled.ContentPaste,
                            contentDescription = stringResource(R.string.action_paste),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.fetch() },
                enabled = url.isNotBlank() && resolveState !is ResolveUiState.Loading,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    if (resolveState is ResolveUiState.Loading) {
                        stringResource(R.string.sheet_loading)
                    } else {
                        stringResource(R.string.action_fetch)
                    },
                )
            }
        }

        PlatformChipsRow(active = viewModel.detectedPlatform)

        clipboardSuggestion?.let { suggestion ->
            AssistChip(
                onClick = viewModel::acceptClipboardSuggestion,
                label = {
                    Text(
                        "${stringResource(R.string.clipboard_detected)}: $suggestion",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = null) },
            )
        }

        if (activeDownloads.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.current_downloads))
                activeDownloads.take(3).forEach { entity ->
                    DownloadRow(
                        entity = entity,
                        onPause = { viewModel.pause(entity.id) },
                        onResume = { viewModel.resume(entity.id) },
                        onRetry = {},
                        onCancel = { viewModel.cancel(entity.id) },
                        onDelete = {},
                        onOpenFile = { Intents.openFile(context, entity) },
                        onOpenFolder = { Intents.openFolder(context, entity) },
                        compact = true,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(stringResource(R.string.quick_actions))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionChip(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.quick_paste_fetch),
                    icon = Icons.Filled.ContentPaste,
                    onClick = { readClipboard(context)?.let(viewModel::fetch) },
                )
                QuickActionChip(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.quick_clear_finished),
                    icon = Icons.Filled.Clear,
                    onClick = {
                        viewModel.onUrlChange("")
                        viewModel.dismissResult()
                    },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Result sheet — the same experience as the share-sheet flow.
    if (resolveState != ResolveUiState.Idle) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissResult,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                when (val state = resolveState) {
                    is ResolveUiState.Loading -> ResolveLoading()
                    is ResolveUiState.Error -> ResolveError(state.message, onRetry = { viewModel.fetch() })
                    is ResolveUiState.Media -> MediaOptionsContent(
                        media = state.media,
                        onPick = { format ->
                            viewModel.pickFormat(format) {
                                Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
                            }
                            viewModel.dismissResult()
                        },
                    )
                    is ResolveUiState.Playlist -> PlaylistContent(
                        playlist = state.playlist,
                        onPickEntry = { entry -> viewModel.fetch(entry.url) },
                        modifier = Modifier.heightIn(max = 480.dp),
                    )
                    ResolveUiState.Idle -> Unit
                }
            }
        }
    }
}

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
}

/** Equal-width quick action pill; label always stays single-line so it can never balloon vertically. */
@Composable
private fun QuickActionChip(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}
