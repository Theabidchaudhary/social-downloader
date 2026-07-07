package app.siphon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.siphon.data.db.DownloadEntity
import app.siphon.data.repo.MediaFormat
import app.siphon.data.repo.ResolveOutcome
import app.siphon.di.AppContainer
import app.siphon.ui.components.ResolveUiState
import app.siphon.util.Platform
import app.siphon.util.UrlDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _resolveState = MutableStateFlow<ResolveUiState>(ResolveUiState.Idle)
    val resolveState: StateFlow<ResolveUiState> = _resolveState.asStateFlow()

    private val _clipboardSuggestion = MutableStateFlow<String?>(null)
    val clipboardSuggestion: StateFlow<String?> = _clipboardSuggestion.asStateFlow()

    val activeDownloads: StateFlow<List<DownloadEntity>> = container.downloadRepository.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val detectedPlatform: Platform? get() = UrlDetector.detect(_url.value)

    private var resolveJob: Job? = null

    fun onUrlChange(value: String) {
        _url.value = value
    }

    fun fetch(target: String = _url.value) {
        val url = target.trim()
        if (url.isEmpty()) return
        _url.value = url
        resolveJob?.cancel()
        _resolveState.value = ResolveUiState.Loading
        resolveJob = viewModelScope.launch {
            _resolveState.value = try {
                when (val outcome = container.mediaRepository.resolve(url)) {
                    is ResolveOutcome.Media -> ResolveUiState.Media(outcome.media)
                    is ResolveOutcome.Playlist -> ResolveUiState.Playlist(outcome.playlist)
                }
            } catch (e: app.siphon.data.remote.ApiException) {
                ResolveUiState.Error(e.message)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ResolveUiState.Error("Couldn't reach the server. Check your connection.")
            }
        }
    }

    fun pickFormat(format: MediaFormat, onEnqueued: () -> Unit) {
        val media = (resolveState.value as? ResolveUiState.Media)?.media ?: return
        viewModelScope.launch {
            container.downloadRepository.enqueue(media, format, container.settings.current())
            onEnqueued()
        }
    }

    fun dismissResult() {
        resolveJob?.cancel()
        _resolveState.value = ResolveUiState.Idle
    }

    /** Called from the screen on resume with the current clipboard text. */
    fun onClipboard(text: String?) {
        viewModelScope.launch {
            if (!container.settings.current().clipboardDetection) return@launch
            val url = text?.let(UrlDetector::extractUrl)
            _clipboardSuggestion.value = if (url != null && UrlDetector.isSupported(url) && url != _url.value) url else null
        }
    }

    fun acceptClipboardSuggestion() {
        _clipboardSuggestion.value?.let {
            _clipboardSuggestion.value = null
            fetch(it)
        }
    }

    fun clearFinished() {
        viewModelScope.launch { container.downloadRepository.clearFinished() }
    }

    fun pause(id: Long) = viewModelScope.launch { container.downloadRepository.pause(id) }
    fun resume(id: Long) = viewModelScope.launch { container.downloadRepository.resume(id) }
    fun cancel(id: Long) = viewModelScope.launch { container.downloadRepository.cancel(id) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container) }
        }
    }
}
