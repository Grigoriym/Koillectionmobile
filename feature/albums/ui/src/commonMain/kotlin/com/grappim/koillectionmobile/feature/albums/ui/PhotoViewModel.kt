package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.photo_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.photo_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the photo. A delete tells the photo's album to read again ([PhotoChanges]).
 *
 * [photoId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class PhotoViewModel(
    @InjectedParam private val photoId: String,
    private val repository: AlbumsRepository,
    private val photoChanges: PhotoChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PhotoUiState(onRetry = ::retry, onDelete = ::delete, onDeleteErrorShown = ::clearDeleteError)
    )
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()

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
            val result = repository.getPhoto(photoId)
            _uiState.update { state ->
                state.copy(
                    photo = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.toMessage(RString.photo_error_unexpected) ?: NativeText.Empty
                )
            }
        }
    }

    private fun delete() {
        val photo = _uiState.value.photo
        if (photo == null || !_uiState.value.canDelete) return
        _uiState.update { it.copy(isDeleting = true, deleteError = NativeText.Empty) }
        viewModelScope.launch {
            val result = repository.deletePhoto(photoId)
            result.onSuccess { photoChanges.notifyChanged(photoId, photo.albumId) }
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.photo_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }
}
