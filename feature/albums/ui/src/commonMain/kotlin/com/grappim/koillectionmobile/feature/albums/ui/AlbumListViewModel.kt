package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The top-level albums. `GET /api/albums` lists the child albums too (M11.2), and the server has no
 * filter for the top level, so the list drops each child. A change to a top-level album
 * ([AlbumChanges] with a null parent) reads the list again, while the screen waits lower on the
 * back stack.
 */
@KoinViewModel
class AlbumListViewModel(private val repository: AlbumsRepository, private val albumChanges: AlbumChanges) :
    ViewModel() {

    private val _uiState = MutableStateFlow(AlbumListUiState(onRetry = ::retry))
    val uiState: StateFlow<AlbumListUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            albumChanges.changed.filter { it.parentId == null }.collect { load() }
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = repository.getAlbums()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { all ->
                        state.copy(
                            albums = all.filter { it.parentId == null }.sortedWith(byTitle { it.title }),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            albums = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.albums_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
