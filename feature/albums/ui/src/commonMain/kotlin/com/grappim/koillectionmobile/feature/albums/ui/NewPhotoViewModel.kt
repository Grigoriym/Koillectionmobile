package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * The new-photo form of album [albumId]: the text fields and the image, which Save needs. The save
 * creates the photo with the fields that have a value, then uploads the image. On success the album
 * screen reads again ([PhotoChanges]).
 *
 * The values and the photo are kept in [savedStateHandle], so they survive process death. Once the
 * photo exists, its id is kept there too, so a retry after a process death uploads the image to it
 * and never creates a second photo.
 *
 * The photo is a cache file ([PhotoFiles]). It is deleted when it is removed or replaced, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 *
 * [albumId] arrives through `parametersOf` at the call site, as in [AlbumViewModel].
 */
@KoinViewModel
class NewPhotoViewModel(
    @InjectedParam private val albumId: String,
    private val repository: AlbumsRepository,
    private val photoFiles: PhotoFiles,
    private val photoChanges: PhotoChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val draft: NewPhotoDraft =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { json.decodeFromString<NewPhotoDraft>(it) }
            ?: NewPhotoDraft()

    private var record: NewPhotoRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<NewPhotoRecord>(it) }
            ?: NewPhotoRecord()

    /** The image bytes of the last save attempt, so a retry does not prepare the photo again. */
    private var pendingImage: ByteArray? = null

    private val _uiState = MutableStateFlow(
        NewPhotoUiState(
            values = draft.values,
            photo = draft.photo,
            isSaved = record.isSaved,
            save = if (record.createdId != null && !record.isSaved) {
                AlbumSaveStatus.Failed(
                    NativeText.Resource(RString.collection_form_save_unfinished),
                    isPartial = true
                )
            } else {
                null
            },
            onFieldChange = ::changeField,
            onPhotosAdd = ::addPhotos,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm
        )
    )
    val uiState: StateFlow<NewPhotoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { NewPhotoDraft(it.values, it.photo) }
                .distinctUntilChanged()
                .collect { savedStateHandle[DRAFT_KEY] = json.encodeToString(it) }
        }
    }

    private fun changeField(field: PhotoField, text: String) {
        _uiState.update { it.copy(values = it.values.with(field, text)) }
    }

    /**
     * The first of [uris] becomes the image and replaces the one before, which is deleted. The
     * other [uris] are not used: the picker can return more than one.
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

    /** Only when nothing exists on the server yet: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is AlbumSaveStatus.Failed || status.isPartial) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val form = _uiState.value
        val values = form.values
        // Save needs a photo (canSave). The form cannot change while a save is shown.
        val photo = form.photo ?: return
        _uiState.update { it.copy(save = if (pendingImage == null) AlbumSaveStatus.Preparing else sending()) }
        viewModelScope.launch {
            val image = pendingImage ?: try {
                photoFiles.prepare(photo.uri, photo.rotation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Any failure to read or encode the photo; the platform part names none in particular.
                logcat(tag = TAG) { "Preparing the photo failed: $e" }
                fail(NativeText.Resource(RString.collection_form_save_error_photo))
                return@launch
            }
            pendingImage = image

            val id = record.createdId ?: run {
                _uiState.update { it.copy(save = sending()) }
                val created = repository.createPhoto(
                    albumId = albumId,
                    title = values.valueOf(PhotoField.TITLE).orEmpty(),
                    comment = values.valueOf(PhotoField.COMMENT),
                    place = values.valueOf(PhotoField.PLACE)
                ).getOrElse {
                    fail(it.toSaveMessage(uploadFailed = false))
                    return@launch
                }
                keepRecord(NewPhotoRecord(createdId = created.id))
                created.id
            }

            _uiState.update { it.copy(save = sending()) }
            repository.uploadPhotoImage(id, image, PHOTO_IMAGE_FILE_NAME, JPEG_MIME_TYPE)
                .onFailure {
                    fail(it.toSaveMessage(uploadFailed = true))
                    return@launch
                }

            keepRecord(NewPhotoRecord(createdId = id, isSaved = true))
            photoFiles.delete(listOf(photo.uri))
            photoChanges.notifyChanged(id, albumId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    /** Two calls: the create, then the upload. */
    private fun sending(): AlbumSaveStatus =
        AlbumSaveStatus.Sending(done = if (record.createdId == null) 0 else 1, total = 2)

    private fun fail(message: NativeText) {
        _uiState.update {
            it.copy(save = AlbumSaveStatus.Failed(message, isPartial = record.createdId != null))
        }
    }

    private fun keepRecord(newRecord: NewPhotoRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }

    override fun onCleared() {
        photoFiles.delete(listOfNotNull(_uiState.value.photo?.uri))
    }
}

/** What [NewPhotoViewModel] keeps of the form across process death. */
@Serializable
private data class NewPhotoDraft(val values: PhotoFormValues = PhotoFormValues(), val photo: FormPhoto? = null)

/**
 * What [NewPhotoViewModel] keeps of a save across process death: [createdId] once the photo exists,
 * and [isSaved] once the save is done.
 */
@Serializable
private data class NewPhotoRecord(val createdId: String? = null, val isSaved: Boolean = false)

internal const val PHOTO_IMAGE_FILE_NAME = "photo.jpg"
private const val DRAFT_KEY = "new_photo_draft"
private const val SAVE_KEY = "new_photo_save"
private const val TAG = "NewPhotoViewModel"

private val json = Json { ignoreUnknownKeys = true }
