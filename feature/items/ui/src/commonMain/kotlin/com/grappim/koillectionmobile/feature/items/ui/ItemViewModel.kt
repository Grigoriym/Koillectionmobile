package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.items.domain.ItemChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.item_error_unexpected
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
 * Reads the item, its fields and its tags in parallel. The tags are sorted by label, as the web UI
 * shows them: `Item::$tags` has `OrderBy(label)`, but the API list does not apply it.
 *
 * A delete tells the item's collection to read its list again ([ItemListChanges]). A save on the
 * edit form ([ItemChanges]) reads the item again.
 *
 * [itemId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class ItemViewModel(
    @InjectedParam private val itemId: String,
    private val itemsRepository: ItemsRepository,
    private val tagsRepository: TagsRepository,
    private val itemListChanges: ItemListChanges,
    private val itemChanges: ItemChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ItemUiState(onRetry = ::retry, onDelete = ::delete, onDeleteErrorShown = ::clearDeleteError)
    )
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    /** The collection of the loaded item, for the change signal after a delete. */
    private var collectionId: String? = null

    init {
        load()
        viewModelScope.launch {
            itemChanges.changedItems.filter { it == itemId }.collect { load() }
        }
    }

    private fun delete() {
        if (!_uiState.value.canDelete) return
        _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }
        viewModelScope.launch {
            val result = itemsRepository.deleteItem(itemId)
            result.onSuccess { collectionId?.let(itemListChanges::notifyChanged) }
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.item_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val item = async { itemsRepository.getItem(itemId) }
            val data = async { itemsRepository.getData(itemId) }
            val tags = async { tagsRepository.getItemTags(itemId) }
            val itemResult = item.await()
            val dataResult = data.await()
            val tagsResult = tags.await()
            val failure = itemResult.exceptionOrNull()
                ?: dataResult.exceptionOrNull()
                ?: tagsResult.exceptionOrNull()
            if (failure == null) collectionId = itemResult.getOrThrow().collectionId
            _uiState.update { state ->
                if (failure != null) {
                    state.copy(
                        name = "",
                        image = null,
                        fields = emptyList(),
                        tags = emptyList(),
                        isLoading = false,
                        error = failure.toMessage(RString.item_error_unexpected)
                    )
                } else {
                    state.copy(
                        name = itemResult.getOrThrow().name,
                        image = itemResult.getOrThrow().image,
                        fields = dataResult.getOrThrow().map { it.toField() },
                        tags = tagsResult.getOrThrow().map { it.label }.sortedWith(String.CASE_INSENSITIVE_ORDER),
                        isLoading = false,
                        error = NativeText.Empty
                    )
                }
            }
        }
    }
}
