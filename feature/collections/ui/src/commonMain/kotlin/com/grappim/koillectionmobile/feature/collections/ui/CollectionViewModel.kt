package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_count_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_error_unexpected
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
 * A change to this collection's items ([ItemListChanges]: a new item, a saved edit, a delete) or
 * to its children ([CollectionListChanges]: a child created or deleted) or to the collection itself
 * ([CollectionChanges]: a saved edit) reads everything again, while the screen waits lower on the
 * back stack.
 *
 * Before the delete confirm, the screen counts what goes with the collection: the server deletes
 * the child collections at every level and their items (`cascade: all` in `Collection.php`).
 * A delete then tells the parent's list to read its children again.
 */
@KoinViewModel
class CollectionViewModel(
    @InjectedParam private val collectionId: String,
    private val collectionsRepository: CollectionsRepository,
    private val itemsRepository: ItemsRepository,
    private val itemListChanges: ItemListChanges,
    private val collectionListChanges: CollectionListChanges,
    private val collectionChanges: CollectionChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CollectionUiState(
            onRetry = ::retry,
            onDeleteClick = ::countForDelete,
            onDeleteConfirm = ::delete,
            onDeleteDismiss = ::dismissDelete,
            onDeleteErrorShown = ::clearDeleteError
        )
    )
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    /** The parent of the loaded collection (null at the top level), for the signal after a delete. */
    private var parentId: String? = null

    init {
        load()
        viewModelScope.launch {
            itemListChanges.changedCollections
                .filter { it == collectionId }
                .collect { load() }
        }
        viewModelScope.launch {
            collectionListChanges.changedParents
                .filter { it == collectionId }
                .collect { load() }
        }
        viewModelScope.launch {
            collectionChanges.changedCollections
                .filter { it == collectionId }
                .collect { load() }
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun countForDelete() {
        if (!_uiState.value.canDelete) return
        _uiState.update { it.copy(isCountingDelete = true, deleteError = NativeText.Empty) }
        viewModelScope.launch {
            val result = countDeleted()
            _uiState.update { state ->
                state.copy(
                    isCountingDelete = false,
                    deleteSummary = result.getOrNull(),
                    deleteError = result.exceptionOrNull()
                        ?.toMessage(RString.collection_delete_count_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    /**
     * One list of every collection (the server has no filter by parent), the descendants found in
     * it by `parentId`, then one item count per collection, in parallel.
     */
    private suspend fun countDeleted(): Result<DeleteSummary> = coroutineScope {
        val all = collectionsRepository.getCollections().getOrElse { return@coroutineScope Result.failure(it) }
        val descendants = descendantIds(collectionId, all)
        val counts = (listOf(collectionId) + descendants)
            .map { id -> async { collectionsRepository.countItems(id) } }
            .awaitAll()
        counts.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { return@coroutineScope Result.failure(it) }
        Result.success(DeleteSummary(childCount = descendants.size, itemCount = counts.sumOf { it.getOrThrow() }))
    }

    private fun dismissDelete() {
        _uiState.update { it.copy(deleteSummary = null) }
    }

    private fun delete() {
        if (_uiState.value.deleteSummary == null) return
        _uiState.update { it.copy(deleteSummary = null, isDeleting = true) }
        viewModelScope.launch {
            val result = collectionsRepository.deleteCollection(collectionId)
            result.onSuccess { collectionListChanges.notifyChanged(parentId) }
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.collection_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
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
            if (failure == null) parentId = collectionResult.getOrThrow().parentId
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

/** The ids of every collection under [rootId] in [all], at every level. [rootId] itself is not in it. */
internal fun descendantIds(rootId: String, all: List<Collection>): List<String> {
    val childrenByParent = all.groupBy { it.parentId }
    val found = mutableListOf<String>()
    val queue = ArrayDeque(listOf(rootId))
    while (queue.isNotEmpty()) {
        childrenByParent[queue.removeFirst()].orEmpty().forEach { child ->
            found += child.id
            queue.addLast(child.id)
        }
    }
    return found
}
