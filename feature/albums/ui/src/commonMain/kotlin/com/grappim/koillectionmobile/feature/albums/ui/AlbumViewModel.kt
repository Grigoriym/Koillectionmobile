package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_error_unexpected
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
 * Reads the album, its children and its photos in parallel. A change to this album or to one of its
 * children ([AlbumChanges]) reads everything again, while the screen waits lower on the back stack.
 *
 * [albumId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class AlbumViewModel(
    @InjectedParam private val albumId: String,
    private val repository: AlbumsRepository,
    private val albumChanges: AlbumChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState(onRetry = ::retry))
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            albumChanges.changed
                .filter { it.albumId == albumId || it.parentId == albumId }
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
            val album = async { repository.getAlbum(albumId) }
            val children = async { repository.getChildren(albumId) }
            val photos = async { repository.getPhotos(albumId) }
            val albumResult = album.await()
            val childrenResult = children.await()
            val photosResult = photos.await()
            val failure = albumResult.exceptionOrNull()
                ?: childrenResult.exceptionOrNull()
                ?: photosResult.exceptionOrNull()
            _uiState.update { state ->
                if (failure != null) {
                    state.copy(
                        title = "",
                        children = emptyList(),
                        photos = emptyList(),
                        isLoading = false,
                        error = failure.toMessage(RString.album_error_unexpected)
                    )
                } else {
                    state.copy(
                        title = albumResult.getOrThrow().title,
                        children = childrenResult.getOrThrow().sortedWith(byTitle { it.title }),
                        photos = photosResult.getOrThrow().sortedWith(byTitle { it.title }),
                        isLoading = false,
                        error = NativeText.Empty
                    )
                }
            }
        }
    }
}
