package app.siphon.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.siphon.R
import app.siphon.SiphonApp
import app.siphon.ui.components.MediaOptionsContent
import app.siphon.ui.components.PlaylistContent
import app.siphon.ui.components.ResolveError
import app.siphon.ui.components.ResolveLoading
import app.siphon.ui.components.ResolveUiState
import app.siphon.ui.theme.SiphonTheme
import app.siphon.util.UrlDetector
import kotlinx.coroutines.launch

/**
 * The share-sheet flow — Siphon's flagship interaction.
 *
 * Appears as a translucent overlay (Theme.Siphon.Share) on top of whatever
 * app fired the share intent. A Compose ModalBottomSheet shows the resolved
 * media and its quality options; one tap enqueues a background download and
 * the overlay dismisses, leaving the user exactly where they were.
 */
@OptIn(ExperimentalMaterial3Api::class)
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels {
        ShareViewModel.factory((application as SiphonApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = extractUrl(intent)
        if (url == null || !UrlDetector.isSupported(url)) {
            Toast.makeText(this, R.string.sheet_error_generic, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        viewModel.resolve(url)

        setContent {
            SiphonTheme {
                val state by viewModel.state.collectAsState()
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                fun dismiss() {
                    scope.launch {
                        sheetState.hide()
                        finish()
                        overridePendingTransition(0, 0)
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { finish(); overridePendingTransition(0, 0) },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                        when (val s = state) {
                            is ResolveUiState.Loading -> ResolveLoading()
                            is ResolveUiState.Error -> ResolveError(s.message, onRetry = viewModel::retry)
                            is ResolveUiState.Media -> MediaOptionsContent(
                                media = s.media,
                                onPick = { format ->
                                    viewModel.pickFormat(format) {
                                        Toast.makeText(
                                            this@ShareActivity,
                                            R.string.download_started,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        dismiss()
                                    }
                                },
                            )
                            is ResolveUiState.Playlist -> PlaylistContent(
                                playlist = s.playlist,
                                onPickEntry = { entry -> viewModel.resolve(entry.url) },
                                modifier = Modifier.heightIn(max = 440.dp),
                            )
                            ResolveUiState.Idle -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun extractUrl(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_SEND -> UrlDetector.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
        Intent.ACTION_VIEW -> intent.dataString
        else -> null
    }
}
