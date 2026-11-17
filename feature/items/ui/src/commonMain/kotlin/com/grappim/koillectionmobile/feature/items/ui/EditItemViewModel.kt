package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.EditItemProgress
import com.grappim.koillectionmobile.feature.items.domain.EditItemResult
import com.grappim.koillectionmobile.feature.items.domain.EditItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.EditedField
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemEdit
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemSnapshot
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.items.domain.NewImage
import com.grappim.koillectionmobile.feature.items.domain.NewImageField
import com.grappim.koillectionmobile.feature.items.domain.NewTextField
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.tags.domain.loadTagGroups
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_save_error_duplicate_label
import com.grappim.koillectionmobile.strings.generated.resources.item_error_unexpected
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
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
 * The edit form of item [itemId]: the add form (plan §5 decision 8), filled from the server. Text
 * and image fields can change; fields of other types show read only. The main image can be
 * replaced, not removed.
 *
 * The item is read once. The loaded values and the edits are kept in [savedStateHandle], so after
 * a process death the form shows the edits and does not read the item again. The tag list is read
 * again.
 *
 * The save sends the edit through [EditItemUseCase], against the item as loaded (an [ItemSnapshot],
 * also kept in [savedStateHandle]). The first try is [EditItemUseCase.editItem]; every later one,
 * after a process death too, is [EditItemUseCase.retry], which reads the item again and sends only
 * what still differs. On success the item screen ([ItemChanges]) and the collection
 * ([ItemListChanges]) read the item again.
 *
 * Photos taken or picked here are cache files ([PhotoFiles]). They are deleted when a photo is
 * removed or replaced again, after a successful save, and when the form leaves the back stack
 * ([onCleared]).
 */
@KoinViewModel
class EditItemViewModel(
    @InjectedParam private val itemId: String,
    private val itemsRepository: ItemsRepository,
    private val tagsRepository: TagsRepository,
    private val editItemUseCase: EditItemUseCase,
    private val photoFiles: PhotoFiles,
    private val itemListChanges: ItemListChanges,
    private val itemChanges: ItemChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: SavedForm? = savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<SavedForm>(it) }

    /** The item as loaded: the base of the save's diff. */
    private var snapshot: ItemSnapshot? =
        savedStateHandle.get<String>(SNAPSHOT_KEY)?.let { json.decodeFromString<SavedSnapshot>(it).toSnapshot() }

    /** A save was started before, so the next one is a retry. */
    private var hasTried: Boolean = savedStateHandle.get<Boolean>(TRIED_KEY) == true

    /** The edit of the last save, so a retry sends the same photos without preparing them again. */
    private var pendingEdit: ItemEdit? = null

    /**
     * A call of an earlier try went through, so the form can no longer change (only a retry).
     * After a process death that is not known, so a started save counts as sent.
     */
    private var hasSent = hasTried

    private var nextId = saved?.draft?.maxId()?.plus(1) ?: 0

    private val _uiState = MutableStateFlow(
        EditItemUiState(
            draft = saved?.draft,
            original = saved?.original,
            isLoading = saved == null,
            onRetry = ::retry,
            onNameChange = ::changeName,
            onAddField = ::addField,
            onRemoveField = ::removeField,
            onLabelChange = ::changeLabel,
            onValueChange = ::changeValue,
            onTagToggle = ::toggleTag,
            onRetryTags = ::retryTags,
            onPhotosAdd = ::addPhotos,
            onPhotoReplace = ::replacePhoto,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onPhotoLabelChange = ::changePhotoLabel,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm,
            // A save that a process death stopped: what went through is not known, so only a retry.
            save = if (hasTried) {
                SaveStatus.Failed(NativeText.Resource(RString.add_item_save_unfinished), isPartial = true)
            } else {
                null
            }
        )
    )
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { state -> state.original?.let { original -> state.draft?.let { SavedForm(original, it) } } }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { savedStateHandle[FORM_KEY] = json.encodeToString(it) }
        }
        if (saved == null) load()
        loadTags()
    }

    private fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.draft != null) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val item = async { itemsRepository.getItem(itemId) }
            val data = async { itemsRepository.getData(itemId) }
            val tags = async { tagsRepository.getItemTags(itemId) }
            val itemResult = item.await()
            val dataResult = data.await()
            val tagsResult = tags.await()
            val failure = itemResult.exceptionOrNull()
                ?: dataResult.exceptionOrNull()
                ?: tagsResult.exceptionOrNull()
            if (failure != null) {
                _uiState.update {
                    it.copy(isLoading = false, error = failure.toMessage(RString.item_error_unexpected))
                }
                return@launch
            }
            snapshot = ItemSnapshot(
                item = itemResult.getOrThrow(),
                data = dataResult.getOrThrow(),
                tagIds = tagsResult.getOrThrow().mapTo(mutableSetOf()) { it.id }
            ).also { savedStateHandle[SNAPSHOT_KEY] = json.encodeToString(SavedSnapshot.of(it)) }
            val loaded = EditItemDraft(
                name = itemResult.getOrThrow().name,
                mainImage = EditPhoto(id = nextId++, previewUrl = itemResult.getOrThrow().image?.previewUrl),
                photos = dataResult.getOrThrow().filter { it.type == TYPE_IMAGE }.map { it.toPhoto() },
                fields = dataResult.getOrThrow().filter { it.type != TYPE_IMAGE }.map { it.toRow() },
                selectedTagIds = tagsResult.getOrThrow().mapTo(mutableSetOf()) { it.id }
            )
            _uiState.update { it.copy(draft = loaded, original = loaded, isLoading = false, error = NativeText.Empty) }
        }
    }

    private fun Datum.toPhoto() =
        EditPhoto(id = nextId++, datumId = id, previewUrl = image?.previewUrl, label = label.orEmpty())

    private fun Datum.toRow(): EditRow = if (type == TYPE_TEXT) {
        EditRow.Text(id = nextId++, datumId = id, label = label.orEmpty(), value = value.orEmpty())
    } else {
        EditRow.ReadOnly(id = nextId++, label = label.orEmpty(), datumType = type, value = value)
    }

    private fun loadTags() {
        viewModelScope.launch {
            val result = tagsRepository.loadTagGroups()
            _uiState.update { state ->
                state.copy(
                    tagGroups = result.getOrElse { emptyList() },
                    isLoadingTags = false,
                    tagsError = result.exceptionOrNull()?.toTagsMessage() ?: NativeText.Empty
                )
            }
        }
    }

    private fun retryTags() {
        if (_uiState.value.isLoadingTags) return
        _uiState.update { it.copy(isLoadingTags = true, tagsError = NativeText.Empty) }
        loadTags()
    }

    private fun changeName(name: String) = updateDraft { it.copy(name = name) }

    private fun addField() = updateDraft { it.copy(fields = it.fields + EditRow.Text(nextId++, null, "", "")) }

    private fun removeField(id: Int) = updateDraft { d ->
        d.copy(fields = d.fields.filterNot { it.id == id && it is EditRow.Text })
    }

    private fun changeLabel(id: Int, label: String) = updateText(id) { it.copy(label = label) }

    private fun changeValue(id: Int, value: String) = updateText(id) { it.copy(value = value) }

    private fun updateText(id: Int, change: (EditRow.Text) -> EditRow.Text) = updateDraft { d ->
        d.copy(fields = d.fields.map { if (it.id == id && it is EditRow.Text) change(it) else it })
    }

    private fun toggleTag(tagId: String) = updateDraft { d ->
        val selected = d.selectedTagIds
        d.copy(selectedTagIds = if (tagId in selected) selected - tagId else selected + tagId)
    }

    /** New image fields, after the existing ones. */
    private fun addPhotos(uris: List<String>) {
        if (_uiState.value.draft == null) {
            photoFiles.delete(uris)
            return
        }
        updateDraft { d -> d.copy(photos = d.photos + uris.map { EditPhoto(id = nextId++, uri = it) }) }
    }

    /**
     * The first of [uris] replaces the image of photo [id] (the main image or an image field). A
     * replacement made before is deleted. The other [uris] are not used: the picker can return
     * more than one.
     */
    private fun replacePhoto(id: Int, uris: List<String>) {
        val target = _uiState.value.draft?.photo(id)
        val uri = uris.firstOrNull()
        if (target == null || uri == null) {
            photoFiles.delete(uris)
            return
        }
        updatePhoto(id) { it.copy(uri = uri, rotation = Rotation.NONE) }
        photoFiles.delete(listOfNotNull(target.uri) + uris.drop(1))
    }

    /** Only a photo from the device turns: the server's image is shown as it is. */
    private fun rotatePhoto(id: Int) = updatePhoto(id) {
        if (it.uri !=
            null
        ) {
            it.copy(rotation = it.rotation.next())
        } else {
            it
        }
    }

    /** Removes image field [id]. The main image cannot be removed (plan §5 decision 8). */
    private fun removePhoto(id: Int) {
        val removed = _uiState.value.draft?.photos?.firstOrNull { it.id == id } ?: return
        updateDraft { d -> d.copy(photos = d.photos.filterNot { it.id == id }) }
        photoFiles.delete(listOfNotNull(removed.uri))
    }

    private fun changePhotoLabel(id: Int, label: String) = updatePhoto(id) { it.copy(label = label) }

    private fun updatePhoto(id: Int, change: (EditPhoto) -> EditPhoto) = updateDraft { d ->
        if (d.mainImage.id == id) {
            d.copy(mainImage = change(d.mainImage))
        } else {
            d.copy(photos = d.photos.map { if (it.id == id) change(it) else it })
        }
    }

    private fun updateDraft(change: (EditItemDraft) -> EditItemDraft) {
        _uiState.update { state -> state.draft?.let { state.copy(draft = change(it)) } ?: state }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave || state.save != null) return
        pendingEdit = null
        runSave()
    }

    private fun retrySave() {
        if (_uiState.value.save !is SaveStatus.Failed) return
        runSave()
    }

    /** Only when no call went through: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is SaveStatus.Failed || status.isPartial) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val draft = _uiState.value.draft ?: return
        val original = snapshot ?: return
        _uiState.update { it.copy(save = SaveStatus.Preparing) }
        viewModelScope.launch {
            val edit = pendingEdit ?: try {
                draft.toEdit()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Any failure to read or encode a photo; the platform part names none in particular.
                logcat(tag = TAG) { "Preparing a photo failed: $e" }
                fail(NativeText.Resource(RString.add_item_save_error_photo))
                return@launch
            }
            pendingEdit = edit

            val isRetry = hasTried
            hasTried = true
            savedStateHandle[TRIED_KEY] = true
            val onProgress: (EditItemProgress) -> Unit = { progress ->
                if (progress.done > 0) hasSent = true
                _uiState.update { it.copy(save = SaveStatus.Sending(progress.done, progress.total)) }
            }
            val result = if (isRetry) {
                editItemUseCase.retry(original, edit, onProgress)
            } else {
                editItemUseCase.editItem(original, edit, onProgress)
            }
            when (result) {
                EditItemResult.Success -> {
                    photoFiles.delete(draft.localUris())
                    original.item.collectionId?.let(itemListChanges::notifyChanged)
                    itemChanges.notifyChanged(itemId)
                    _uiState.update { it.copy(isSaved = true) }
                }

                is EditItemResult.DuplicateLabel ->
                    fail(NativeText.Arguments(RString.edit_item_save_error_duplicate_label, listOf(result.label)))

                is EditItemResult.Failure -> fail(result.error.toSaveMessage(uploadFailed = result.isUpload))
            }
        }
    }

    private fun fail(message: NativeText) {
        _uiState.update { it.copy(save = SaveStatus.Failed(message, isPartial = hasSent)) }
    }

    /**
     * The form as the use case takes it. Text rows and image fields with a server id are existing
     * fields; the rest are new. A photo from the device is prepared; the server's images are not
     * sent. Throws when a photo cannot be prepared.
     */
    private suspend fun EditItemDraft.toEdit(): ItemEdit {
        var photoIndex = 0
        suspend fun EditPhoto.newImage(): NewImage? = uri?.let {
            NewImage(photoFiles.prepare(it, rotation), "photo-${photoIndex++}.jpg", JPEG_MIME_TYPE)
        }
        val texts = fields.filterIsInstance<EditRow.Text>()
        val (existingPhotos, newPhotos) = photos.partition { it.datumId != null }
        return ItemEdit(
            name = name.trim(),
            fields = texts.mapNotNull { row -> row.datumId?.let { EditedField.Text(it, row.label, row.value) } } +
                existingPhotos.map { EditedField.Image(checkNotNull(it.datumId), it.label, it.newImage()) },
            newTextFields = texts.filter { it.datumId == null }.map { NewTextField(it.label, it.value) },
            newImageFields = newPhotos.map { NewImageField(it.label, checkNotNull(it.newImage())) },
            newMainImage = mainImage.newImage(),
            tagIds = selectedTagIds.toList()
        )
    }

    override fun onCleared() {
        photoFiles.delete(_uiState.value.draft?.localUris().orEmpty())
    }
}

/** What [EditItemViewModel] keeps across process death. */
@Serializable
private data class SavedForm(val original: EditItemDraft, val draft: EditItemDraft)

private fun EditItemDraft.photo(id: Int): EditPhoto? = if (mainImage.id ==
    id
) {
    mainImage
} else {
    photos.firstOrNull { it.id == id }
}

private fun EditItemDraft.maxId(): Int = (listOf(mainImage.id) + photos.map { it.id } + fields.map { it.id }).max()

private fun EditItemDraft.localUris(): List<String> = (listOf(mainImage) + photos).mapNotNull { it.uri }

/** The large thumbnail, or the full image when the server sent no thumbnail, as on the item screen. */
private val Image.previewUrl: String get() = largeThumbnailUrl ?: url

/** The types the form edits; the same as `EditItemUseCaseImpl`'s. */
private const val TYPE_TEXT = "text"
private const val TYPE_IMAGE = "image"

private const val FORM_KEY = "edit_item_form"
private const val SNAPSHOT_KEY = "edit_item_snapshot"
private const val TRIED_KEY = "edit_item_tried"
private const val TAG = "EditItemViewModel"

/** [ItemSnapshot] in saved state: the domain classes are not serializable. */
@Serializable
private data class SavedSnapshot(
    val id: String,
    val name: String,
    val quantity: Int,
    val collectionId: String?,
    val image: SavedImage?,
    val data: List<SavedDatum>,
    val tagIds: Set<String>
) {
    fun toSnapshot() = ItemSnapshot(
        item = Item(id, name, quantity, collectionId, image?.toImage()),
        data = data.map { Datum(it.id, it.type, it.label, it.value, it.position, it.image?.toImage()) },
        tagIds = tagIds
    )

    companion object {
        fun of(snapshot: ItemSnapshot) = with(snapshot.item) {
            SavedSnapshot(
                id = id,
                name = name,
                quantity = quantity,
                collectionId = collectionId,
                image = image?.let(SavedImage::of),
                data = snapshot.data.map {
                    SavedDatum(it.id, it.type, it.label, it.value, it.position, it.image?.let(SavedImage::of))
                },
                tagIds = snapshot.tagIds
            )
        }
    }
}

@Serializable
private data class SavedDatum(
    val id: String,
    val type: String,
    val label: String?,
    val value: String?,
    val position: Int?,
    val image: SavedImage?
)

@Serializable
private data class SavedImage(val url: String, val smallThumbnailUrl: String?, val largeThumbnailUrl: String?) {
    fun toImage() = Image(url, smallThumbnailUrl, largeThumbnailUrl)

    companion object {
        fun of(image: Image) = SavedImage(image.url, image.smallThumbnailUrl, image.largeThumbnailUrl)
    }
}

private val json = Json { ignoreUnknownKeys = true }
