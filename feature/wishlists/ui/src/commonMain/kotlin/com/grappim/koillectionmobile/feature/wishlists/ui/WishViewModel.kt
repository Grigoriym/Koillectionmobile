package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wish_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wish_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the wish, and again when the edit form saved it ([WishChanges]). A delete tells the wish's
 * wishlist to read again; this screen ignores that signal.
 *
 * [wishId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class WishViewModel(
    @InjectedParam private val wishId: String,
    private val repository: WishlistsRepository,
    private val wishChanges: WishChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WishUiState(onRetry = ::retry, onDelete = ::delete, onDeleteErrorShown = ::clearDeleteError)
    )
    val uiState: StateFlow<WishUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            wishChanges.changed
                .filter { it.wishId == wishId && !_uiState.value.isDeleting && !_uiState.value.isDeleted }
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
            val result = repository.getWish(wishId)
            _uiState.update { state ->
                state.copy(
                    wish = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.toMessage(RString.wish_error_unexpected) ?: NativeText.Empty
                )
            }
        }
    }

    private fun delete() {
        val wish = _uiState.value.wish
        if (wish == null || !_uiState.value.canDelete) return
        _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }
        viewModelScope.launch {
            val result = repository.deleteWish(wishId)
            result.onSuccess { wishChanges.notifyChanged(wishId, wish.wishlistId) }
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.wish_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }
}
