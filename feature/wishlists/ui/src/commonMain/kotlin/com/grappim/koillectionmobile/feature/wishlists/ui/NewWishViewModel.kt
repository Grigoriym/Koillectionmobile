package com.grappim.koillectionmobile.feature.wishlists.ui

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
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
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
 * The new-wish form of wishlist [wishlistId]: the text fields and an optional image. The save
 * creates the wish with the fields that have a value, then uploads the image. On success the
 * wishlist screen reads again ([WishChanges]).
 *
 * The values and the photo are kept in [savedStateHandle], so they survive process death. Once the
 * wish exists, its id is kept there too, so a retry after a process death uploads the image to it
 * and never creates a second wish.
 *
 * The photo is a cache file ([PhotoFiles]). It is deleted when it is removed or replaced, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 *
 * [wishlistId] arrives through `parametersOf` at the call site, as in [WishlistViewModel].
 */
@KoinViewModel
class NewWishViewModel(
    @InjectedParam private val wishlistId: String,
    private val repository: WishlistsRepository,
    private val photoFiles: PhotoFiles,
    private val wishChanges: WishChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val draft: NewWishDraft =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { json.decodeFromString<NewWishDraft>(it) } ?: NewWishDraft()

    private var record: NewWishRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<NewWishRecord>(it) } ?: NewWishRecord()

    /** The image bytes of the last save attempt, so a retry does not prepare the photo again. */
    private var pendingImage: ByteArray? = null

    private val _uiState = MutableStateFlow(
        NewWishUiState(
            values = draft.values,
            photo = draft.photo,
            isSaved = record.isSaved,
            save = if (record.createdId != null && !record.isSaved) {
                FormSaveStatus.Failed(
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
    val uiState: StateFlow<NewWishUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { NewWishDraft(it.values, it.photo) }
                .distinctUntilChanged()
                .collect { savedStateHandle[DRAFT_KEY] = json.encodeToString(it) }
        }
    }

    private fun changeField(field: WishField, text: String) {
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
        if (_uiState.value.save !is FormSaveStatus.Failed) return
        runSave()
    }

    /** Only when nothing exists on the server yet: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is FormSaveStatus.Failed || status.isPartial) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val form = _uiState.value
        val values = form.values
        val photo = form.photo
        val total = if (photo == null) 1 else 2
        _uiState.update {
            it.copy(save = if (photo != null && pendingImage == null) FormSaveStatus.Preparing else sending(total))
        }
        viewModelScope.launch {
            val image = if (photo == null) {
                null
            } else {
                pendingImage ?: try {
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
            pendingImage = image

            val id = record.createdId ?: run {
                _uiState.update { it.copy(save = sending(total)) }
                val created = repository.createWish(
                    wishlistId = wishlistId,
                    name = values.valueOf(WishField.NAME).orEmpty(),
                    url = values.valueOf(WishField.URL),
                    price = values.valueOf(WishField.PRICE),
                    currency = values.valueOf(WishField.CURRENCY),
                    comment = values.valueOf(WishField.COMMENT)
                ).getOrElse {
                    fail(it.toSaveMessage(uploadFailed = false))
                    return@launch
                }
                keepRecord(NewWishRecord(createdId = created.id))
                created.id
            }

            if (image != null) {
                _uiState.update { it.copy(save = FormSaveStatus.Sending(done = 1, total = total)) }
                repository.uploadWishImage(id, image, WISH_IMAGE_FILE_NAME, JPEG_MIME_TYPE)
                    .onFailure {
                        fail(it.toSaveMessage(uploadFailed = true))
                        return@launch
                    }
            }

            keepRecord(NewWishRecord(createdId = id, isSaved = true))
            photoFiles.delete(listOfNotNull(photo?.uri))
            wishChanges.notifyChanged(id, wishlistId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun sending(total: Int): FormSaveStatus =
        FormSaveStatus.Sending(done = if (record.createdId == null) 0 else 1, total = total)

    private fun fail(message: NativeText) {
        _uiState.update {
            it.copy(save = FormSaveStatus.Failed(message, isPartial = record.createdId != null))
        }
    }

    private fun keepRecord(newRecord: NewWishRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }

    override fun onCleared() {
        photoFiles.delete(listOfNotNull(_uiState.value.photo?.uri))
    }
}

/** What [NewWishViewModel] keeps of the form across process death. */
@Serializable
private data class NewWishDraft(val values: WishFormValues = WishFormValues(), val photo: FormPhoto? = null)

/**
 * What [NewWishViewModel] keeps of a save across process death: [createdId] once the wish exists,
 * and [isSaved] once the save is done.
 */
@Serializable
private data class NewWishRecord(val createdId: String? = null, val isSaved: Boolean = false)

internal const val WISH_IMAGE_FILE_NAME = "wish.jpg"
private const val DRAFT_KEY = "new_wish_draft"
private const val SAVE_KEY = "new_wish_save"
private const val TAG = "NewWishViewModel"

private val json = Json { ignoreUnknownKeys = true }
