package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.item_search_error_unexpected
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Koillection 1.8.5 has no name filter on items (plan §5 decision 10). The screen reads every
 * item once when it opens, and the collection list in parallel for the collection titles: an item
 * names its collection by IRI only. The query then filters in the app; it sends no call. It is kept
 * in [savedStateHandle], so it survives process death.
 *
 * A change to any item list ([ItemListChanges]: a new item, a saved edit, a delete) reads
 * everything again, so a deleted item does not stay in the results.
 */
@KoinViewModel
class ItemSearchViewModel(
    private val itemsRepository: ItemsRepository,
    private val collectionsRepository: CollectionsRepository,
    private val itemListChanges: ItemListChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ItemSearchUiState(
            query = savedStateHandle.get<String>(QUERY_KEY).orEmpty(),
            onQueryChange = ::changeQuery,
            onRetry = ::retry
        )
    )
    val uiState: StateFlow<ItemSearchUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            itemListChanges.changedCollections.collect { load() }
        }
    }

    private fun changeQuery(query: String) {
        savedStateHandle[QUERY_KEY] = query
        _uiState.update { it.copy(query = query) }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val items = async { itemsRepository.getAllItems() }
            val collections = async { collectionsRepository.getCollections() }
            val itemsResult = items.await()
            val collectionsResult = collections.await()
            val failure = itemsResult.exceptionOrNull() ?: collectionsResult.exceptionOrNull()
            _uiState.update { state ->
                if (failure != null) {
                    state.copy(
                        results = emptyList(),
                        isLoading = false,
                        error = failure.toMessage(RString.item_search_error_unexpected)
                    )
                } else {
                    val titles = collectionsResult.getOrThrow().associate { it.id to it.title }
                    state.copy(
                        results = itemsResult.getOrThrow().map { item ->
                            ItemSearchResult(
                                itemId = item.id,
                                name = item.name,
                                collectionTitle = item.collectionId?.let(titles::get)
                            )
                        },
                        isLoading = false,
                        error = NativeText.Empty
                    )
                }
            }
        }
    }
}

private const val QUERY_KEY = "item_search_query"
