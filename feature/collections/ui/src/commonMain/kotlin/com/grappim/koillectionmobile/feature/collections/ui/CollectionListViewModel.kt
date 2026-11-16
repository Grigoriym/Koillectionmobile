package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel

/**
 * The top-level collections: the server has no filter for them, so the list drops each child.
 * A change to the top-level list ([CollectionListChanges] with a null parent) reads it again,
 * while the screen waits lower on the back stack.
 *
 * Log out does not navigate: clearing the session turns `SessionStorage.isLoggedIn` `false`, and
 * the app shell swaps to the login screen.
 */
@KoinViewModel
class CollectionListViewModel(
    private val repository: CollectionsRepository,
    private val authRepository: AuthRepository,
    private val collectionListChanges: CollectionListChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CollectionListUiState(onRefresh = ::refresh, onLogOutClick = ::logOut)
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

    private fun logOut() {
        viewModelScope.launch { authRepository.logOut() }
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
                        state.copy(isLoading = false, isRefreshing = false, error = error.toMessage())
                    }
                )
            }
        }
    }
}

internal fun Throwable.toMessage(unexpected: StringResource = RString.collections_error_unexpected): NativeText =
    when (this) {
        is KoillectionError.Network -> NativeText.Resource(RString.collections_error_network)
        is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
        is KoillectionError.Server -> NativeText.Arguments(RString.collections_error_http, listOf(status))
        is KoillectionError.Http -> NativeText.Arguments(RString.collections_error_http, listOf(status))
        else -> NativeText.Resource(unexpected)
    }
