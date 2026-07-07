package app.siphon.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.siphon.data.db.DownloadEntity
import app.siphon.data.db.DownloadStatus
import app.siphon.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadFilter { ALL, ACTIVE, QUEUED, COMPLETED, FAILED, PAUSED }

enum class DownloadSort { NEWEST, OLDEST, TITLE, SIZE }

class DownloadsViewModel(private val container: AppContainer) : ViewModel() {

    private val _filter = MutableStateFlow(DownloadFilter.ALL)
    val filter: StateFlow<DownloadFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(DownloadSort.NEWEST)
    val sort: StateFlow<DownloadSort> = _sort.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val downloads: StateFlow<List<DownloadEntity>> =
        combine(container.downloadRepository.all, _filter, _sort, _query) { all, filter, sort, query ->
            all.asSequence()
                .filter { entity ->
                    when (filter) {
                        DownloadFilter.ALL -> true
                        DownloadFilter.ACTIVE -> entity.status == DownloadStatus.RUNNING
                        DownloadFilter.QUEUED -> entity.status == DownloadStatus.QUEUED
                        DownloadFilter.COMPLETED -> entity.status == DownloadStatus.COMPLETED
                        DownloadFilter.FAILED ->
                            entity.status == DownloadStatus.FAILED || entity.status == DownloadStatus.CANCELED
                        DownloadFilter.PAUSED -> entity.status == DownloadStatus.PAUSED
                    }
                }
                .filter { entity ->
                    query.isBlank() || entity.title.contains(query, ignoreCase = true) ||
                        entity.platform.contains(query, ignoreCase = true)
                }
                .sortedWith(
                    when (sort) {
                        DownloadSort.NEWEST -> compareByDescending { it.createdAt }
                        DownloadSort.OLDEST -> compareBy { it.createdAt }
                        DownloadSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                        DownloadSort.SIZE -> compareByDescending { maxOf(it.totalBytes, it.downloadedBytes) }
                    },
                )
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(value: DownloadFilter) {
        _filter.value = value
    }

    fun setSort(value: DownloadSort) {
        _sort.value = value
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun pause(id: Long) = viewModelScope.launch { container.downloadRepository.pause(id) }
    fun resume(id: Long) = viewModelScope.launch { container.downloadRepository.resume(id) }
    fun retry(id: Long) = viewModelScope.launch { container.downloadRepository.retry(id) }
    fun cancel(id: Long) = viewModelScope.launch { container.downloadRepository.cancel(id) }
    fun delete(id: Long, deleteFile: Boolean) =
        viewModelScope.launch { container.downloadRepository.delete(id, deleteFile) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DownloadsViewModel(container) }
        }
    }
}
