package app.siphon.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.siphon.data.remote.ApiException
import app.siphon.data.repo.MediaFormat
import app.siphon.data.repo.ResolveOutcome
import app.siphon.di.AppContainer
import app.siphon.ui.components.ResolveUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShareViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow<ResolveUiState>(ResolveUiState.Loading)
    val state: StateFlow<ResolveUiState> = _state.asStateFlow()

    private var currentUrl: String? = null

    fun resolve(url: String) {
        currentUrl = url
        _state.value = ResolveUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                when (val outcome = container.mediaRepository.resolve(url)) {
                    is ResolveOutcome.Media -> ResolveUiState.Media(outcome.media)
                    is ResolveOutcome.Playlist -> ResolveUiState.Playlist(outcome.playlist)
                }
            } catch (e: ApiException) {
                ResolveUiState.Error(e.message)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ResolveUiState.Error("Couldn't reach the server. Check your connection.")
            }
        }
    }

    fun retry() {
        currentUrl?.let(::resolve)
    }

    /**
     * Enqueue and hand off to the background service. The share overlay
     * closes immediately after; the user never leaves the app they were in.
     */
    fun pickFormat(format: MediaFormat, onEnqueued: () -> Unit) {
        val media = (_state.value as? ResolveUiState.Media)?.media ?: return
        viewModelScope.launch {
            container.downloadRepository.enqueue(media, format, container.settings.current())
            onEnqueued()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ShareViewModel(container) }
        }
    }
}
