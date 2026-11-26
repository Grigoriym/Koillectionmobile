package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.photo_error_unexpected
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
 * The edit form of photo [photoId]: the text fields and the image, filled from the server. The save
 * sends one `PATCH` with only the changed fields ([PhotoFormValues.changesFrom]; a cleared field
 * goes out as `null`), then uploads a new image.
 *
 * The photo is read once. The loaded values and the edits are kept in [savedStateHandle], so after
 * a process death the form shows the edits and does not read the photo again. The calls that went
 * through are kept there too, so a retry sends only the rest.
 *
 * On success the photo screen and its album screen read again ([PhotoChanges]).
 *
 * The photo is a cache file ([PhotoFiles]). It is deleted when it is removed or replaced, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 */
@KoinViewModel
class EditPhotoViewModel(
    @InjectedParam private val photoId: String,
    private val repository: AlbumsRepository,
    private val photoFiles: PhotoFiles,
    private val photoChanges: PhotoChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: EditPhotoForm? =
        savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<EditPhotoForm>(it) }

    private var record: EditPhotoRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<EditPhotoRecord>(it) }
            ?: EditPhotoRecord()

    /** The image bytes of the last save attempt, so a retry does not prepare the photo again. */
    private var pendingImage: ByteArray? = null

    private val _uiState = MutableStateFlow(
        EditPhotoUiState(
            original = saved?.original,
            isLoading = saved == null,
            values = saved?.values ?: PhotoFormValues(),
            photo = saved?.photo,
            isSaved = record.isSaved,
            // A save that a process death stopped: what went through is not known, so only a retry.
            save = if (record.isStarted && !record.isSaved) {
                AlbumSaveStatus.Failed(
                    NativeText.Resource(RString.collection_form_save_unfinished),
                    isPartial = true
                )
            } else {
                null
            },
            onRetry = ::retry,
            onFieldChange = ::changeField,
            onPhotosAdd = ::addPhotos,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm
        )
    )
    val uiState: StateFlow<EditPhotoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { s -> s.original?.let { EditPhotoForm(it, s.values, s.photo) } }
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
            repository.getPhoto(photoId)
                .onSuccess { photo ->
                    val loaded = LoadedPhoto(photo.toFormValues(), photo.albumId, photo.imageUrl)
                    _uiState.update {
                        it.copy(original = loaded, values = loaded.values, isLoading = false, error = NativeText.Empty)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.toMessage(RString.photo_error_unexpected))
                    }
                }
        }
    }

    private fun changeField(field: PhotoField, text: String) {
        _uiState.update { it.copy(values = it.values.with(field, text)) }
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

    /** Drops the new photo only: the server image stays, as the API cannot remove it. */
    private fun removePhoto() {
        val old = _uiState.value.photo ?: return
        _uiState.update { it.copy(photo = null) }
        photoFiles.delete(listOf(old.uri))
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave || state.save != null) return
        pendingImage = null
        runSave()
    }

    private fun retrySave() {
        if (_uiState.value.save !is AlbumSaveStatus.Failed) return
        runSave()
    }

    /** Only when no call went through: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is AlbumSaveStatus.Failed || status.isPartial) return
        keepRecord(EditPhotoRecord())
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val form = _uiState.value
        val original = form.original ?: return
        val steps = form.changes
        val photo = form.photo
        keepRecord(record.copy(isStarted = true))
        val needsImage = PhotoEditStep.UPLOAD_IMAGE in steps && PhotoEditStep.UPLOAD_IMAGE !in record.done
        _uiState.update {
            it.copy(save = if (needsImage && pendingImage == null) AlbumSaveStatus.Preparing else sending(steps))
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
                    PhotoEditStep.EDIT -> repository.editPhoto(photoId, form.values.changesFrom(original.values))

                    PhotoEditStep.UPLOAD_IMAGE -> repository.uploadPhotoImage(
                        photoId,
                        checkNotNull(pendingImage),
                        PHOTO_IMAGE_FILE_NAME,
                        JPEG_MIME_TYPE
                    )
                }
                result.onFailure {
                    fail(it.toSaveMessage(uploadFailed = step == PhotoEditStep.UPLOAD_IMAGE))
                    return@launch
                }
                keepRecord(record.copy(done = record.done + step))
            }

            keepRecord(record.copy(isSaved = true))
            photoFiles.delete(listOfNotNull(photo?.uri))
            photoChanges.notifyChanged(photoId, original.albumId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun sending(steps: List<PhotoEditStep>): AlbumSaveStatus =
        AlbumSaveStatus.Sending(done = steps.count { it in record.done }, total = steps.size)

    private fun fail(message: NativeText) {
        _uiState.update {
            it.copy(save = AlbumSaveStatus.Failed(message, isPartial = record.done.isNotEmpty()))
        }
    }

    private fun keepRecord(newRecord: EditPhotoRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }

    override fun onCleared() {
        photoFiles.delete(listOfNotNull(_uiState.value.photo?.uri))
    }
}

/** What [EditPhotoViewModel] keeps of the form across process death. */
@Serializable
private data class EditPhotoForm(val original: LoadedPhoto, val values: PhotoFormValues, val photo: FormPhoto?)

/**
 * What [EditPhotoViewModel] keeps of a save across process death: [isStarted] once a save began,
 * the [done] calls, and [isSaved] once every call went through.
 */
@Serializable
private data class EditPhotoRecord(
    val isStarted: Boolean = false,
    val done: Set<PhotoEditStep> = emptySet(),
    val isSaved: Boolean = false
)

private const val FORM_KEY = "edit_photo_form"
private const val SAVE_KEY = "edit_photo_save"
private const val TAG = "EditPhotoViewModel"

private val json = Json { ignoreUnknownKeys = true }
