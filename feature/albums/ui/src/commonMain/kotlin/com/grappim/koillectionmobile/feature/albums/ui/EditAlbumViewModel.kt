package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import com.grappim.koillectionmobile.core.form.toSaveMessage
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * The edit form of album [albumId]: the title and the image, filled from the server. The image can
 * be replaced or removed. The save sends only what changed ([EditAlbumUiState.changes]), one call
 * per change.
 *
 * The album is read once. The loaded values and the edits are kept in [savedStateHandle], so after
 * a process death the form shows the edits and does not read the album again. The calls that went
 * through are kept there too, so a retry sends only the rest.
 *
 * On success the album screen and the screen that lists the album read again ([AlbumChanges]).
 *
 * The photo is a cache file ([PhotoFiles]). It is deleted when it is removed or replaced, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 */
@KoinViewModel
class EditAlbumViewModel(
    @InjectedParam private val albumId: String,
    private val repository: AlbumsRepository,
    private val photoFiles: PhotoFiles,
    private val albumChanges: AlbumChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: EditAlbumForm? =
        savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<EditAlbumForm>(it) }

    private var record: EditAlbumRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<EditAlbumRecord>(it) }
            ?: EditAlbumRecord()

    /** The image bytes of the last save attempt, so a retry does not prepare the photo again. */
    private var pendingImage: ByteArray? = null

    private val _uiState = MutableStateFlow(
        EditAlbumUiState(
            original = saved?.original,
            isLoading = saved == null,
            title = saved?.title.orEmpty(),
            photo = saved?.photo,
            imageUrl = saved?.imageUrl,
            isSaved = record.isSaved,
            // A save that a process death stopped: what went through is not known, so only a retry.
            save = if (record.isStarted && !record.isSaved) {
                FormSaveStatus.Failed(
                    NativeText.Resource(RString.collection_form_save_unfinished),
                    isPartial = true
                )
            } else {
                null
            },
            onRetry = ::retry,
            onTitleChange = ::changeTitle,
            onPhotosAdd = ::addPhotos,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm
        )
    )
    val uiState: StateFlow<EditAlbumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { s -> s.original?.let { EditAlbumForm(it, s.title, s.photo, s.imageUrl) } }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { savedStateHandle[FORM_KEY] = json.encodeToString(it) }
        }
        if (saved == null) load()
    }

    private fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.original != null) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            repository.getAlbum(albumId)
                .onSuccess { album ->
                    val loaded = LoadedAlbum(album.title, album.parentId, album.imageUrl)
                    _uiState.update {
                        it.copy(
                            original = loaded,
                            title = loaded.title,
                            imageUrl = loaded.imageUrl,
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.toMessage(RString.album_error_unexpected))
                    }
                }
        }
    }

    private fun changeTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    /**
     * The first of [uris] becomes the new image. A photo picked before is deleted. The other [uris]
     * are not used: the picker can return more than one.
     */
    private fun addPhotos(uris: List<String>) {
        val uri = uris.firstOrNull() ?: return
        val old = _uiState.value.photo
        _uiState.update { it.copy(photo = FormPhoto(uri)) }
        photoFiles.delete(listOfNotNull(old?.uri) + uris.drop(1))
    }

    private fun rotatePhoto() {
        _uiState.update { s -> s.copy(photo = s.photo?.let { it.copy(rotation = it.rotation.next()) }) }
    }

    /** "Remove image" leaves the album without an image: the new photo and the server image both go. */
    private fun removePhoto() {
        val old = _uiState.value.photo
        _uiState.update { it.copy(photo = null, imageUrl = null) }
        photoFiles.delete(listOfNotNull(old?.uri))
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave || state.save != null) return
        pendingImage = null
        runSave()
    }

    private fun retrySave() {
        if (_uiState.value.save !is FormSaveStatus.Failed) return
        runSave()
    }

    /** Only when no call went through: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is FormSaveStatus.Failed || status.isPartial) return
        keepRecord(EditAlbumRecord())
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val form = _uiState.value
        val original = form.original ?: return
        val steps = form.changes
        val photo = form.photo
        keepRecord(record.copy(isStarted = true))
        val needsImage = AlbumEditStep.UPLOAD_IMAGE in steps && AlbumEditStep.UPLOAD_IMAGE !in record.done
        _uiState.update {
            it.copy(save = if (needsImage && pendingImage == null) FormSaveStatus.Preparing else sending(steps))
        }
        viewModelScope.launch {
            if (needsImage && photo != null && pendingImage == null) {
                pendingImage = try {
                    photoFiles.prepare(photo.uri, photo.rotation)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Any failure to read or encode the photo; the platform part names none in particular.
                    logcat(tag = TAG) { "Preparing the photo failed: $e" }
                    fail(NativeText.Resource(RString.collection_form_save_error_photo))
                    return@launch
                }
            }

            for (step in steps) {
                if (step in record.done) continue
                _uiState.update { it.copy(save = sending(steps)) }
                val result = when (step) {
                    AlbumEditStep.RENAME -> repository.renameAlbum(albumId, form.title.trim())

                    AlbumEditStep.UPLOAD_IMAGE -> repository.uploadAlbumImage(
                        albumId,
                        checkNotNull(pendingImage),
                        IMAGE_FILE_NAME,
                        JPEG_MIME_TYPE
                    )

                    AlbumEditStep.DELETE_IMAGE -> repository.deleteAlbumImage(albumId)
                }
                result.onFailure {
                    fail(it.toSaveMessage(uploadFailed = step == AlbumEditStep.UPLOAD_IMAGE))
                    return@launch
                }
                keepRecord(record.copy(done = record.done + step))
            }

            keepRecord(record.copy(isSaved = true))
            photoFiles.delete(listOfNotNull(photo?.uri))
            albumChanges.notifyChanged(albumId, original.parentId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun sending(steps: List<AlbumEditStep>): FormSaveStatus =
        FormSaveStatus.Sending(done = steps.count { it in record.done }, total = steps.size)

    private fun fail(message: NativeText) {
        _uiState.update {
            it.copy(save = FormSaveStatus.Failed(message, isPartial = record.done.isNotEmpty()))
        }
    }

    private fun keepRecord(newRecord: EditAlbumRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }

    override fun onCleared() {
        photoFiles.delete(listOfNotNull(_uiState.value.photo?.uri))
    }
}

/** What [EditAlbumViewModel] keeps of the form across process death. */
@Serializable
private data class EditAlbumForm(
    val original: LoadedAlbum,
    val title: String,
    val photo: FormPhoto?,
    val imageUrl: String?
)

/**
 * What [EditAlbumViewModel] keeps of a save across process death: [isStarted] once a save began,
 * the [done] calls, and [isSaved] once every call went through.
 */
@Serializable
private data class EditAlbumRecord(
    val isStarted: Boolean = false,
    val done: Set<AlbumEditStep> = emptySet(),
    val isSaved: Boolean = false
)

private const val FORM_KEY = "edit_album_form"
private const val SAVE_KEY = "edit_album_save"
private const val TAG = "EditAlbumViewModel"

private val json = Json { ignoreUnknownKeys = true }
