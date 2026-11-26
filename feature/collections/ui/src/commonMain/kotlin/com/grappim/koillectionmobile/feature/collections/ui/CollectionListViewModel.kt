package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The top-level collections: the server has no filter for them, so the list drops each child.
 * A change to the top-level list ([CollectionListChanges] with a null parent) reads it again,
 * while the screen waits lower on the back stack.
 */
@KoinViewModel
class CollectionListViewModel(
    private val repository: CollectionsRepository,
    private val collectionListChanges: CollectionListChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CollectionListUiState(onRefresh = ::refresh)
    )
    val uiState: StateFlow<CollectionListUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            collectionListChanges.changedParents.filter { it == null }.collect { load() }
        }
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
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.toMessage(RString.collections_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
