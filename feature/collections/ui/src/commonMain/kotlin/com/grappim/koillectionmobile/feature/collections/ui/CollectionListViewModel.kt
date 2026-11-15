package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** The top-level collections: the server has no filter for them, so the list drops each child. */
@KoinViewModel
class CollectionListViewModel(private val repository: CollectionsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionListUiState(onRefresh = ::refresh))
    val uiState: StateFlow<CollectionListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun refresh() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = repository.getCollections()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { all ->
                        state.copy(
                            collections = all.filter { it.parentId == null },
                            isLoading = false,
                            isRefreshing = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = { error ->
                        state.copy(isLoading = false, isRefreshing = false, error = error.toMessage())
                    }
                )
            }
        }
    }
}

internal fun Throwable.toMessage(): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.collections_error_network)
    is KoillectionError.Server -> NativeText.Arguments(RString.collections_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.collections_error_http, listOf(status))
    else -> NativeText.Resource(RString.collections_error_unexpected)
}
