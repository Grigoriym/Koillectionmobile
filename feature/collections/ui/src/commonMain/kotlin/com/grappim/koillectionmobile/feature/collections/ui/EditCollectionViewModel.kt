package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.collections.domain.CollectionChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_templates_error
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_unexpected
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
 * The edit form of collection [collectionId]: the title, the image and the default template, filled
 * from the server. The image can be replaced or removed. The default template is picked from the
 * server's templates, or none. The save sends only what changed
 * ([EditCollectionUiState.changes]), one call per change.
 *
 * The collection is read once. The loaded values and the edits are kept in [savedStateHandle], so
 * after a process death the form shows the edits and does not read the collection again. The
 * calls that went through are kept there too, so a retry sends only the rest. The template list is
 * read again after a process death.
 *
 * On success the collection screen ([CollectionChanges]) and the list that shows the collection
 * ([CollectionListChanges], by its parent) read it again.
 *
 * The photo is a cache file ([PhotoFiles]). It is deleted when it is removed or replaced, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 */
@KoinViewModel
class EditCollectionViewModel(
    @InjectedParam private val collectionId: String,
    private val collectionsRepository: CollectionsRepository,
    private val templatesRepository: TemplatesRepository,
    private val photoFiles: PhotoFiles,
    private val collectionListChanges: CollectionListChanges,
    private val collectionChanges: CollectionChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: EditCollectionForm? =
        savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<EditCollectionForm>(it) }

    private var record: EditCollectionRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<EditCollectionRecord>(it) }
            ?: EditCollectionRecord()

    /** The image bytes of the last save attempt, so a retry does not prepare the photo again. */
    private var pendingImage: ByteArray? = null

    private val _uiState = MutableStateFlow(
        EditCollectionUiState(
            original = saved?.original,
            isLoading = saved == null,
            title = saved?.title.orEmpty(),
            photo = saved?.photo,
            imageUrl = saved?.imageUrl,
            defaultTemplateId = saved?.defaultTemplateId,
            isSaved = record.isSaved,
            // A save that a process death stopped: what went through is not known, so only a retry.
            save = if (record.isStarted && !record.isSaved) {
                CollectionSaveStatus.Failed(
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
            onDefaultTemplateChange = ::changeDefaultTemplate,
            onRetryTemplates = ::retryTemplates,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm
        )
    )
    val uiState: StateFlow<EditCollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { s ->
                    s.original?.let { EditCollectionForm(it, s.title, s.photo, s.imageUrl, s.defaultTemplateId) }
                }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { savedStateHandle[FORM_KEY] = json.encodeToString(it) }
        }
        if (saved == null) load()
        loadTemplates()
    }

    private fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.original != null) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            collectionsRepository.getCollection(collectionId)
                .onSuccess { collection ->
                    val loaded = LoadedCollection(
                        collection.title,
                        collection.parentId,
                        collection.imageUrl,
                        collection.defaultTemplateId
                    )
                    _uiState.update {
                        it.copy(
                            original = loaded,
                            title = loaded.title,
                            imageUrl = loaded.imageUrl,
                            defaultTemplateId = loaded.defaultTemplateId,
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.toMessage(RString.collections_error_unexpected))
                    }
                }
        }
    }

    private fun changeTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    /** [templateId] null: the collection gets no default template. */
    private fun changeDefaultTemplate(templateId: String?) {
        _uiState.update { it.copy(defaultTemplateId = templateId) }
    }

    private fun retryTemplates() {
        if (_uiState.value.templates.isLoading) return
        _uiState.update { it.copy(templates = TemplatesUi()) }
        loadTemplates()
    }

    /** Sorted by name, ignoring case, as the template list. */
    private fun loadTemplates() {
        viewModelScope.launch {
            val result = templatesRepository.getTemplates()
            _uiState.update { state ->
                state.copy(
                    templates = result.fold(
                        onSuccess = { templates ->
                            TemplatesUi(
                                templates = templates.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                                isLoading = false
                            )
                        },
                        onFailure = {
                            TemplatesUi(
                                isLoading = false,
                                error = NativeText.Resource(RString.collection_form_templates_error)
                            )
                        }
                    )
                )
            }
        }
    }

    /**
     * The first of [uris] becomes the new image. A photo picked before is deleted. The other
     * [uris] are not used: the picker can return more than one.
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

    /** "Remove image" leaves the collection without an image: the new photo and the server image both go. */
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
        if (_uiState.value.save !is CollectionSaveStatus.Failed) return
        runSave()
    }

    /** Only when no call went through: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is CollectionSaveStatus.Failed || status.isPartial) return
        keepRecord(EditCollectionRecord())
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val form = _uiState.value
        val original = form.original ?: return
        val steps = form.changes
        val photo = form.photo
        keepRecord(record.copy(isStarted = true))
        val needsImage = CollectionEditStep.UPLOAD_IMAGE in steps && CollectionEditStep.UPLOAD_IMAGE !in record.done
        _uiState.update {
            it.copy(
                save = if (needsImage && pendingImage == null) {
                    CollectionSaveStatus.Preparing
                } else {
                    sending(steps)
                }
            )
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
                    CollectionEditStep.RENAME -> collectionsRepository.renameCollection(collectionId, form.title.trim())

                    CollectionEditStep.SET_DEFAULT_TEMPLATE ->
                        collectionsRepository.setDefaultTemplate(collectionId, form.defaultTemplateId)

                    CollectionEditStep.UPLOAD_IMAGE -> collectionsRepository.uploadCollectionImage(
                        collectionId,
                        checkNotNull(pendingImage),
                        IMAGE_FILE_NAME,
                        JPEG_MIME_TYPE
                    )

                    CollectionEditStep.DELETE_IMAGE -> collectionsRepository.deleteCollectionImage(collectionId)
                }
                result.onFailure {
                    fail(it.toSaveMessage(uploadFailed = step == CollectionEditStep.UPLOAD_IMAGE))
                    return@launch
                }
                keepRecord(record.copy(done = record.done + step))
            }

            keepRecord(record.copy(isSaved = true))
            photoFiles.delete(listOfNotNull(photo?.uri))
            collectionListChanges.notifyChanged(original.parentId)
            collectionChanges.notifyChanged(collectionId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun sending(steps: List<CollectionEditStep>): CollectionSaveStatus =
        CollectionSaveStatus.Sending(done = steps.count { it in record.done }, total = steps.size)

    private fun fail(message: NativeText) {
        _uiState.update {
            it.copy(save = CollectionSaveStatus.Failed(message, isPartial = record.done.isNotEmpty()))
        }
    }

    private fun keepRecord(newRecord: EditCollectionRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }

    override fun onCleared() {
        photoFiles.delete(listOfNotNull(_uiState.value.photo?.uri))
    }
}

/** What [EditCollectionViewModel] keeps of the form across process death. */
@Serializable
private data class EditCollectionForm(
    val original: LoadedCollection,
    val title: String,
    val photo: FormPhoto?,
    val imageUrl: String?,
    val defaultTemplateId: String? = null
)

/**
 * What [EditCollectionViewModel] keeps of a save across process death: [isStarted] once a save
 * began, the [done] calls, and [isSaved] once every call went through.
 */
@Serializable
private data class EditCollectionRecord(
    val isStarted: Boolean = false,
    val done: Set<CollectionEditStep> = emptySet(),
    val isSaved: Boolean = false
)

private const val FORM_KEY = "edit_collection_form"
private const val SAVE_KEY = "edit_collection_save"
private const val IMAGE_FILE_NAME = "collection.jpg"
private const val TAG = "EditCollectionViewModel"

private val json = Json { ignoreUnknownKeys = true }
