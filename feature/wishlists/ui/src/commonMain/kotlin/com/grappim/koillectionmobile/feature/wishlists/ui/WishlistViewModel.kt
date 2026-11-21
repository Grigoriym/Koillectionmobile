package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_error_unexpected
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
 * Reads the wishlist, its children and its wishes in parallel. A change to this wishlist or to one
 * of its children ([WishlistChanges]) reads everything again, while the screen waits lower on the
 * back stack.
 *
 * [wishlistId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class WishlistViewModel(
    @InjectedParam private val wishlistId: String,
    private val repository: WishlistsRepository,
    private val wishlistChanges: WishlistChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(WishlistUiState(onRetry = ::retry))
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            wishlistChanges.changed
                .filter { it.wishlistId == wishlistId || it.parentId == wishlistId }
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
            val wishlist = async { repository.getWishlist(wishlistId) }
            val children = async { repository.getChildren(wishlistId) }
            val wishes = async { repository.getWishes(wishlistId) }
            val wishlistResult = wishlist.await()
            val childrenResult = children.await()
            val wishesResult = wishes.await()
            val failure = wishlistResult.exceptionOrNull()
                ?: childrenResult.exceptionOrNull()
                ?: wishesResult.exceptionOrNull()
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
