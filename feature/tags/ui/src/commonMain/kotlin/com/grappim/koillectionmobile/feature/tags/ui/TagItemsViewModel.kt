package com.grappim.koillectionmobile.feature.tags.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.tag_items_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the items of tag [tagId] when the screen opens. [tagId] arrives through `parametersOf` at
 * the call site.
 *
 * A change to any item list ([ItemListChanges]: a new item, a saved edit, a delete) reads the
 * items again, so a deleted item, or an item whose tag was taken off, leaves the list.
 */
@KoinViewModel
class TagItemsViewModel(
    @InjectedParam private val tagId: String,
    private val itemsRepository: ItemsRepository,
    private val itemListChanges: ItemListChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagItemsUiState(onRetry = ::retry))
    val uiState: StateFlow<TagItemsUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            itemListChanges.changedCollections.collect { load() }
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = itemsRepository.getTagItems(tagId)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { items ->
                        state.copy(
                            items = items.map { TagItem(itemId = it.id, name = it.name) },
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            items = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.tag_items_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
