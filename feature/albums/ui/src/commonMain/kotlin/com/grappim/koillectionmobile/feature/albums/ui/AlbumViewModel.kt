package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_count_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.album_error_unexpected
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
 * Reads the album, its children and its photos in parallel. A change to this album or to one of its
 * children ([AlbumChanges]), or to one of its photos ([PhotoChanges]), reads everything again,
 * while the screen waits lower on the back stack.
 *
 * Before the delete confirm, the screen counts what goes with the album: the server deletes the
 * child albums at every level and their photos (`cascade: ['all']` in `Album.php`). A delete then
 * tells the list or the parent to read again.
 *
 * [albumId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class AlbumViewModel(
    @InjectedParam private val albumId: String,
    private val repository: AlbumsRepository,
    private val albumChanges: AlbumChanges,
    private val photoChanges: PhotoChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AlbumUiState(
            onRetry = ::retry,
            onDeleteClick = ::countForDelete,
            onDeleteConfirm = ::delete,
            onDeleteDismiss = ::dismissDelete,
            onDeleteErrorShown = ::clearDeleteError
        )
    )
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    /** The parent of the loaded album (null at the top level), for the signal after a delete. */
    private var parentId: String? = null

    init {
        load()
        viewModelScope.launch {
            albumChanges.changed
                .filter { !_uiState.value.isDeleted && (it.albumId == albumId || it.parentId == albumId) }
                .collect { load() }
        }
        viewModelScope.launch {
            photoChanges.changed
                .filter { !_uiState.value.isDeleted && it.albumId == albumId }
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
                        ?.toMessage(RString.album_delete_count_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    /**
     * One list of every album (it holds the children too), the descendants found in it by
     * `parentId`, then the photos of each album, in parallel. `Album.php` has no photo counter, so
     * each read takes every page of photos.
     */
    private suspend fun countDeleted(): Result<DeleteSummary> = coroutineScope {
        val all = repository.getAlbums().getOrElse { return@coroutineScope Result.failure(it) }
        val descendants = descendantIds(albumId, all)
        val photos = (listOf(albumId) + descendants)
            .map { id -> async { repository.getPhotos(id) } }
            .awaitAll()
        photos.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { return@coroutineScope Result.failure(it) }
        Result.success(
            DeleteSummary(childCount = descendants.size, photoCount = photos.sumOf { it.getOrThrow().size })
        )
    }

    private fun dismissDelete() {
        _uiState.update { it.copy(deleteSummary = null) }
    }

    private fun delete() {
        if (_uiState.value.deleteSummary == null) return
        _uiState.update { it.copy(deleteSummary = null, isDeleting = true) }
        viewModelScope.launch {
            val result = repository.deleteAlbum(albumId)
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.album_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
            result.onSuccess { albumChanges.notifyChanged(albumId, parentId) }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
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
            if (failure == null) parentId = albumResult.getOrThrow().parentId
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

/** The ids of every album under [rootId] in [all], at every level. [rootId] itself is not in it. */
internal fun descendantIds(rootId: String, all: List<Album>): List<String> {
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
