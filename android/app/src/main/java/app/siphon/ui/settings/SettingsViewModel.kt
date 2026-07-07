package app.siphon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.siphon.data.settings.SiphonSettings
import app.siphon.data.settings.TargetDir
import app.siphon.data.settings.ThemeMode
import app.siphon.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, FAILED }

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<SiphonSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SiphonSettings())

    private val _connectionTestState = MutableStateFlow(ConnectionTestState.IDLE)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    fun testConnection(url: String) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.TESTING
            _connectionTestState.value =
                if (container.api.ping(url)) ConnectionTestState.SUCCESS else ConnectionTestState.FAILED
        }
    }

    fun resetConnectionTestState() {
        _connectionTestState.value = ConnectionTestState.IDLE
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.settings.setTheme(mode) }

    fun setTargetDir(dir: TargetDir, customTreeUri: String? = null) =
        viewModelScope.launch { container.settings.setTargetDir(dir, customTreeUri) }

    fun setMaxParallel(count: Int) = viewModelScope.launch { container.settings.setMaxParallel(count) }

    fun setClipboardDetection(enabled: Boolean) =
        viewModelScope.launch { container.settings.setClipboardDetection(enabled) }

    fun setNotificationsEnabled(enabled: Boolean) =
        viewModelScope.launch { container.settings.setNotificationsEnabled(enabled) }

    fun setLanguageTag(tag: String) = viewModelScope.launch { container.settings.setLanguageTag(tag) }

    fun setApiBaseUrl(url: String) = viewModelScope.launch { container.settings.setApiBaseUrl(url) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
