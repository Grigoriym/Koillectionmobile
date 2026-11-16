package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the collection, its children and its items in parallel. `getItems` reads every page
 * before it returns, so the items set the wait.
 *
 * [collectionId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 *
 * A change to this collection's items ([ItemListChanges]: a new item, a saved edit, a delete)
 * reads everything again, while the screen waits lower on the back stack.
 */
@KoinViewModel
class CollectionViewModel(
    @InjectedParam private val collectionId: String,
    private val collectionsRepository: CollectionsRepository,
    private val itemsRepository: ItemsRepository,
    private val itemListChanges: ItemListChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState(onRetry = ::retry))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            itemListChanges.changedCollections
                .filter { it == collectionId }
                .collect { load() }
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val collection = async { collectionsRepository.getCollection(collectionId) }
            val children = async { collectionsRepository.getChildren(collectionId) }
            val items = async { itemsRepository.getItems(collectionId) }
            val collectionResult = collection.await()
            val childrenResult = children.await()
            val itemsResult = items.await()
            val failure = collectionResult.exceptionOrNull()
                ?: childrenResult.exceptionOrNull()
                ?: itemsResult.exceptionOrNull()
            _uiState.update { state ->
                if (failure != null) {
                    state.copy(
                        title = "",
                        children = emptyList(),
                        items = emptyList(),
                        isLoading = false,
                        error = failure.toMessage()
                    )
                } else {
                    state.copy(
                        title = collectionResult.getOrThrow().title,
                        children = childrenResult.getOrThrow(),
                        items = itemsResult.getOrThrow(),
                        isLoading = false,
                        error = NativeText.Empty
                    )
                }
            }
        }
    }
}
