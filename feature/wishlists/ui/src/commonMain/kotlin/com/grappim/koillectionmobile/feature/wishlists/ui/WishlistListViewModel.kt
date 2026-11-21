package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The top-level wishlists. `GET /api/wishlists` lists the child wishlists too (M9.2), and the
 * server has no filter for the top level, so the list drops each child.
 */
@KoinViewModel
class WishlistListViewModel(private val repository: WishlistsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WishlistListUiState(onRetry = ::retry))
    val uiState: StateFlow<WishlistListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = repository.getWishlists()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { all ->
                        state.copy(
                            wishlists = all.filter { it.parentId == null }.sortedWith(byName { it.name }),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            wishlists = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.wishlists_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
