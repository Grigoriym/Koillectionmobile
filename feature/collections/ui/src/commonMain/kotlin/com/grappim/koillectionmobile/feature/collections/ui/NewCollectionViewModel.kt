package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_validation
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
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
 * The new-collection form: a title and an optional image (plan §5 decision 9). The save creates
 * the collection, as a child of [parentId] or at the top level when it is null, then uploads the
 * image. The server picks the color.
 *
 * The title and the photo are kept in [savedStateHandle], so they survive process death. Once
 * the collection exists, its id is kept there too, so a retry after a process death uploads the
 * image to it and never creates a second collection.
 *
 * The photo is a cache file ([PhotoFiles]). It is deleted when it is removed or replaced, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 *
 * [parentId] arrives through `parametersOf` at the call site, as in `CollectionViewModel`.
 */
@KoinViewModel
class NewCollectionViewModel(
    @InjectedParam private val parentId: String?,
    private val collectionsRepository: CollectionsRepository,
    private val photoFiles: PhotoFiles,
    private val collectionListChanges: CollectionListChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val draft: NewCollectionDraft =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { json.decodeFromString<NewCollectionDraft>(it) }
            ?: NewCollectionDraft()

    private var record: NewCollectionRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<NewCollectionRecord>(it) }
            ?: NewCollectionRecord()

    /** The image bytes of the last save attempt, so a retry does not prepare the photo again. */
    private var pendingImage: ByteArray? = null

    private val _uiState = MutableStateFlow(
        NewCollectionUiState(
            title = draft.title,
            photo = draft.photo,
            savedCollectionId = record.savedCollectionId,
            save = if (record.createdId != null && record.savedCollectionId == null) {
                CollectionSaveStatus.Failed(
                    NativeText.Resource(RString.collection_form_save_unfinished),
                    isPartial = true
                )
            } else {
                null
            },
            onTitleChange = ::changeTitle,
            onPhotosAdd = ::addPhotos,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm
        )
    )
    val uiState: StateFlow<NewCollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { NewCollectionDraft(it.title, it.photo) }
                .distinctUntilChanged()
                .collect { savedStateHandle[DRAFT_KEY] = json.encodeToString(it) }
        }
    }

    private fun changeTitle(title: String) {
        _uiState.update { it.copy(title = title) }
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
        if (_uiState.value.save !is CollectionSaveStatus.Failed) return
        runSave()
    }

    /** Only when nothing exists on the server yet: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is CollectionSaveStatus.Failed || status.isPartial) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val form = _uiState.value
        val photo = form.photo
        val total = if (photo == null) 1 else 2
        _uiState.update {
            it.copy(
                save = if (photo != null &&
                    pendingImage == null
                ) {
                    CollectionSaveStatus.Preparing
                } else {
                    sending(total)
                }
            )
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
                val created = collectionsRepository.createCollection(form.title.trim(), parentId)
                    .getOrElse {
                        fail(it.toSaveMessage(uploadFailed = false))
                        return@launch
                    }
                keepRecord(NewCollectionRecord(createdId = created.id))
                created.id
            }

            if (image != null) {
                _uiState.update { it.copy(save = CollectionSaveStatus.Sending(done = 1, total = total)) }
                collectionsRepository.uploadCollectionImage(id, image, IMAGE_FILE_NAME, JPEG_MIME_TYPE)
                    .onFailure {
                        fail(it.toSaveMessage(uploadFailed = true))
                        return@launch
                    }
            }

            keepRecord(NewCollectionRecord(createdId = id, savedCollectionId = id))
            photoFiles.delete(listOfNotNull(photo?.uri))
            collectionListChanges.notifyChanged(parentId)
            _uiState.update { it.copy(savedCollectionId = id) }
        }
    }

    private fun sending(total: Int): CollectionSaveStatus = if (record.createdId == null) {
        CollectionSaveStatus.Sending(done = 0, total = total)
    } else {
        CollectionSaveStatus.Sending(done = 1, total = total)
    }

    private fun fail(message: NativeText) {
        _uiState.update {
            it.copy(save = CollectionSaveStatus.Failed(message, isPartial = record.createdId != null))
        }
    }

    private fun keepRecord(newRecord: NewCollectionRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }

    override fun onCleared() {
        photoFiles.delete(listOfNotNull(_uiState.value.photo?.uri))
    }
}

/** What [NewCollectionViewModel] keeps of the form across process death. */
@Serializable
private data class NewCollectionDraft(val title: String = "", val photo: FormPhoto? = null)

/**
 * What [NewCollectionViewModel] keeps of a save across process death: [createdId] once the
 * collection exists, and [savedCollectionId] once the save is done.
 */
@Serializable
private data class NewCollectionRecord(val createdId: String? = null, val savedCollectionId: String? = null)

/** An upload 500 usually means a full upload quota (plan §2), so that one is named. */
internal fun Throwable.toSaveMessage(uploadFailed: Boolean): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.collection_form_save_error_network)

    is KoillectionError.Server -> if (uploadFailed && status == HTTP_INTERNAL_ERROR) {
        NativeText.Resource(RString.collection_form_save_error_quota)
    } else {
        NativeText.Arguments(RString.collection_form_save_error_http, listOf(status))
    }

    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))

    is KoillectionError.Http -> NativeText.Arguments(RString.collection_form_save_error_http, listOf(status))

    is KoillectionError.Unauthorized ->
        NativeText.Arguments(RString.collection_form_save_error_http, listOf(HTTP_UNAUTHORIZED))

    is KoillectionError.NotFound -> NativeText.Arguments(
        RString.collection_form_save_error_http,
        listOf(HTTP_NOT_FOUND)
    )

    is KoillectionError.Validation -> NativeText.Resource(RString.collection_form_save_error_validation)

    else -> NativeText.Resource(RString.collection_form_save_error_unexpected)
}

private const val DRAFT_KEY = "new_collection_draft"
private const val SAVE_KEY = "new_collection_save"
private const val IMAGE_FILE_NAME = "collection.jpg"
private const val TAG = "NewCollectionViewModel"
private const val HTTP_INTERNAL_ERROR = 500
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

private val json = Json { ignoreUnknownKeys = true }
