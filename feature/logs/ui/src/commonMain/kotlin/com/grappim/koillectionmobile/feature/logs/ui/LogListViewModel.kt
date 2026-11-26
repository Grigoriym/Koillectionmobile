package com.grappim.koillectionmobile.feature.logs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.logs.domain.LogEntry
import com.grappim.koillectionmobile.feature.logs.domain.LogsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.logs_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The user's logs, newest first, one page at a time (plan §5 decision 16). The repository gives
 * each page newest first and the path of the page after it. A row whose id the list already
 * shows is dropped: a new log on the server shifts the pages between two reads.
 */
@KoinViewModel
class LogListViewModel(private val repository: LogsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LogListUiState(onRetry = ::retry, onLoadMore = ::loadMore, onRetryMore = ::retryMore)
    )
    val uiState: StateFlow<LogListUiState> = _uiState.asStateFlow()

    /** The page to read next; null before the first page arrives and after the oldest page. */
    private var nextPath: String? = null

    init {
        loadFirst()
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        loadFirst()
    }

    private fun loadFirst() {
        viewModelScope.launch {
            repository.getLogs(null)
                .onSuccess { page ->
                    nextPath = page.nextPath
                    _uiState.update {
                        it.copy(logs = emptyList<LogEntry>().plusNew(page.logs), isLoading = false)
                    }
                }
                .onFailure { e ->
                    nextPath = null
                    _uiState.update {
                        it.copy(
                            logs = emptyList(),
                            isLoading = false,
                            error = e.toMessage(RString.logs_error_unexpected)
                        )
                    }
                }
        }
    }

    private fun loadMore() {
        if (_uiState.value.moreError.isNotEmpty()) return
        retryMore()
    }

    private fun retryMore() {
        val state = _uiState.value
        val path = nextPath
        if (state.isLoading || state.isLoadingMore || path == null) return
        _uiState.update { it.copy(isLoadingMore = true, moreError = NativeText.Empty) }
        viewModelScope.launch {
            repository.getLogs(path)
                .onSuccess { page ->
                    nextPath = page.nextPath
                    _uiState.update { it.copy(logs = it.logs.plusNew(page.logs), isLoadingMore = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingMore = false, moreError = e.toMessage(RString.logs_error_unexpected))
                    }
                }
        }
    }
}

/** [this] followed by the rows of [page] whose id is not in the list yet. */
private fun List<LogEntry>.plusNew(page: List<LogEntry>): List<LogEntry> {
    val shown = mapTo(HashSet()) { it.id }
    return this + page.filter { shown.add(it.id) }
}
