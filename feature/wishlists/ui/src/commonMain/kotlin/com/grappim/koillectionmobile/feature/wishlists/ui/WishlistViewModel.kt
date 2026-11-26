package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.DeleteSummary
import com.grappim.koillectionmobile.core.ui.descendantIds
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_count_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_error_unexpected
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
 * Reads the wishlist, its children and its wishes in parallel. A change to this wishlist or to one
 * of its children ([WishlistChanges]), or to one of its wishes ([WishChanges]), reads everything
 * again, while the screen waits lower on the back stack.
 *
 * Before the delete confirm, the screen counts what goes with the wishlist: the server deletes the
 * child wishlists at every level and their wishes (`cascade: ['all']` in `Wishlist.php`). A delete
 * then tells the list or the parent to read again.
 *
 * [wishlistId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class WishlistViewModel(
    @InjectedParam private val wishlistId: String,
    private val repository: WishlistsRepository,
    private val wishlistChanges: WishlistChanges,
    private val wishChanges: WishChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WishlistUiState(
            onRetry = ::retry,
            onDeleteClick = ::countForDelete,
            onDeleteConfirm = ::delete,
            onDeleteDismiss = ::dismissDelete,
            onDeleteErrorShown = ::clearDeleteError
        )
    )
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    /** The parent of the loaded wishlist (null at the top level), for the signal after a delete. */
    private var parentId: String? = null

    init {
        load()
        viewModelScope.launch {
            wishlistChanges.changed
                .filter { !_uiState.value.isDeleted && (it.wishlistId == wishlistId || it.parentId == wishlistId) }
                .collect { load() }
        }
        viewModelScope.launch {
            wishChanges.changed
                .filter { !_uiState.value.isDeleted && it.wishlistId == wishlistId }
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
                        ?.toMessage(RString.wishlist_delete_count_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    /**
     * One list of every wishlist (it holds the children too), the descendants found in it by
     * `parentId`, then the wishes of each wishlist, in parallel. The API has no wish counter, so
     * each read takes every page of wishes.
     */
    private suspend fun countDeleted(): Result<DeleteSummary> = coroutineScope {
        val all = repository.getWishlists().getOrElse { return@coroutineScope Result.failure(it) }
        val descendants = descendantIds(wishlistId, all.map { it.id to it.parentId })
        val wishes = (listOf(wishlistId) + descendants)
            .map { id -> async { repository.getWishes(id) } }
            .awaitAll()
        wishes.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { return@coroutineScope Result.failure(it) }
        Result.success(
            DeleteSummary(
                childCount = descendants.size,
                contentCount = wishes.sumOf {
                    it.getOrThrow().size
                }
            )
        )
    }

    private fun dismissDelete() {
        _uiState.update { it.copy(deleteSummary = null) }
    }

    private fun delete() {
        if (_uiState.value.deleteSummary == null) return
        _uiState.update { it.copy(deleteSummary = null, isDeleting = true) }
        viewModelScope.launch {
            val result = repository.deleteWishlist(wishlistId)
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.wishlist_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
            result.onSuccess { wishlistChanges.notifyChanged(wishlistId, parentId) }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }

    private fun load() {
        viewModelScope.launch {
            val wishlist = async { repository.getWishlist(wishlistId) }
            val children = async { repository.getChildren(wishlistId) }
            val wishes = async { repository.getWishes(wishlistId) }
            val wishlistResult = wishlist.await()
            val childrenResult = children.await()
            val wishesResult = wishes.await()
            val failure = wishlistResult.exceptionOrNull()
                ?: childrenResult.exceptionOrNull()
                ?: wishesResult.exceptionOrNull()
            if (failure == null) parentId = wishlistResult.getOrThrow().parentId
            _uiState.update { state ->
                if (failure != null) {
                    state.copy(
                        name = "",
                        children = emptyList(),
                        wishes = emptyList(),
                        isLoading = false,
                        error = failure.toMessage(RString.wishlist_error_unexpected)
                    )
                } else {
                    state.copy(
                        name = wishlistResult.getOrThrow().name,
                        children = childrenResult.getOrThrow().sortedWith(byName { it.name }),
                        wishes = wishesResult.getOrThrow().sortedWith(byName { it.name }),
                        isLoading = false,
                        error = NativeText.Empty
                    )
                }
            }
        }
    }
}
