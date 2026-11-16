package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.AddItemResult
import com.grappim.koillectionmobile.feature.items.domain.AddItemState
import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.NewImage
import com.grappim.koillectionmobile.feature.items.domain.NewImageField
import com.grappim.koillectionmobile.feature.items.domain.NewItem
import com.grappim.koillectionmobile.feature.items.domain.NewTextField
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_network
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_validation
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 * The add-item form: name, free label/value rows, photos, and a pick from the server's existing
 * tags. The form cannot create a tag (plan §5 decision 7). The save creates the item in
 * [collectionId] through [AddItemUseCase].
 *
 * What the user typed and picked is kept in [savedStateHandle], so it survives process death. The
 * tags are loaded again. A started save keeps what exists on the server there too, so a retry
 * after a process death never creates a second item.
 *
 * The photos are cache files ([PhotoFiles]). They are deleted when a photo is removed, after a
 * successful save, and when the form leaves the back stack ([onCleared]).
 *
 * [collectionId] arrives through `parametersOf` at the call site, as in `CollectionViewModel`.
 */
@KoinViewModel
class AddItemViewModel(
    @InjectedParam private val collectionId: String,
    private val tagsRepository: TagsRepository,
    private val addItemUseCase: AddItemUseCase,
    private val photoFiles: PhotoFiles,
    private val itemListChanges: ItemListChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val draft: AddItemDraft =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { json.decodeFromString<AddItemDraft>(it) } ?: AddItemDraft()

    private val saveRecord: SaveRecord? =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<SaveRecord>(it) }

    /** What exists on the server of the save in progress, or of the failed one. */
    private var checkpoint: AddItemState = saveRecord?.toState() ?: AddItemState()

    /** The item of the last save attempt, so a retry sends the same item without preparing the photos again. */
    private var pendingItem: NewItem? = null

    private var nextFieldId = (draft.fields.maxOfOrNull { it.id } ?: -1) + 1
    private var nextPhotoId = (draft.photos.maxOfOrNull { it.id } ?: -1) + 1

    private val _uiState = MutableStateFlow(
        AddItemUiState(
            name = draft.name,
            fields = draft.fields,
            photos = draft.photos,
            selectedTagIds = draft.selectedTagIds,
            onNameChange = ::changeName,
            onAddField = ::addField,
            onRemoveField = ::removeField,
            onLabelChange = ::changeLabel,
            onValueChange = ::changeValue,
            onTagToggle = ::toggleTag,
            onRetryTags = ::retryTags,
            onPhotosAdd = ::addPhotos,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onPhotoMakeMain = ::makeMain,
            onPhotoLabelChange = ::changePhotoLabel,
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm,
            savedItemId = saveRecord?.savedItemId,
            save = if (saveRecord?.itemId != null && saveRecord.savedItemId == null) {
                SaveStatus.Failed(NativeText.Resource(RString.add_item_save_unfinished), isPartial = true)
            } else {
                null
            }
        )
    )
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { AddItemDraft(it.name, it.fields, it.photos, it.selectedTagIds) }
                .distinctUntilChanged()
                .collect { savedStateHandle[DRAFT_KEY] = json.encodeToString(it) }
        }
        loadTags()
    }

    private fun addPhotos(uris: List<String>) {
        val rows = uris.map { PhotoRow(id = nextPhotoId++, uri = it) }
        _uiState.update { it.copy(photos = it.photos + rows) }
    }

    private fun rotatePhoto(id: Int) = updatePhoto(id) { it.copy(rotation = it.rotation.next()) }

    private fun removePhoto(id: Int) {
        val removed = _uiState.value.photos.filter { it.id == id }
        _uiState.update { s -> s.copy(photos = s.photos.filterNot { it.id == id }) }
        photoFiles.delete(removed.map { it.uri })
    }

    /** Moves photo [id] to the front: the first photo is the main image. */
    private fun makeMain(id: Int) {
        _uiState.update { s ->
            val (main, rest) = s.photos.partition { it.id == id }
            s.copy(photos = main + rest)
        }
    }

    private fun changePhotoLabel(id: Int, label: String) = updatePhoto(id) { it.copy(label = label) }

    private fun updatePhoto(id: Int, change: (PhotoRow) -> PhotoRow) {
        _uiState.update { s -> s.copy(photos = s.photos.map { if (it.id == id) change(it) else it }) }
    }

    private fun changeName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun addField() {
        val row = FieldRow(id = nextFieldId++)
        _uiState.update { it.copy(fields = it.fields + row) }
    }

    private fun removeField(id: Int) {
        _uiState.update { s -> s.copy(fields = s.fields.filterNot { it.id == id }) }
    }

    private fun changeLabel(id: Int, label: String) = updateField(id) { it.copy(label = label) }

    private fun changeValue(id: Int, value: String) = updateField(id) { it.copy(value = value) }

    private fun updateField(id: Int, change: (FieldRow) -> FieldRow) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == id) change(it) else it }) }
    }

    private fun toggleTag(tagId: String) {
        _uiState.update { s ->
            val selected = s.selectedTagIds
            s.copy(selectedTagIds = if (tagId in selected) selected - tagId else selected + tagId)
        }
    }

    private fun retryTags() {
        if (_uiState.value.isLoadingTags) return
        _uiState.update { it.copy(isLoadingTags = true, tagsError = NativeText.Empty) }
        loadTags()
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave || state.save != null) return
        checkpoint = AddItemState()
        pendingItem = null
        runSave()
    }

    private fun retrySave() {
        if (_uiState.value.save !is SaveStatus.Failed) return
        runSave()
    }

    /** Only when nothing exists on the server yet: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is SaveStatus.Failed || status.isPartial) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        _uiState.update { it.copy(save = SaveStatus.Preparing) }
        viewModelScope.launch {
            val form = _uiState.value
            val newItem = pendingItem ?: try {
                form.toNewItem()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Any failure to read or encode a photo; the platform part names none in particular.
                logcat(tag = TAG) { "Preparing a photo failed: $e" }
                fail(NativeText.Resource(RString.add_item_save_error_photo))
                return@launch
            }
            pendingItem = newItem

            val result = addItemUseCase.addItem(newItem, checkpoint) { progress ->
                keepCheckpoint(progress.state)
                _uiState.update { it.copy(save = SaveStatus.Sending(progress.done, progress.total)) }
            }
            when (result) {
                is AddItemResult.Success -> {
                    savedStateHandle[SAVE_KEY] = json.encodeToString(SaveRecord(savedItemId = result.itemId))
                    photoFiles.delete(form.photos.map { it.uri })
                    itemListChanges.notifyChanged(collectionId)
                    _uiState.update { it.copy(savedItemId = result.itemId) }
                }

                is AddItemResult.Failure -> {
                    keepCheckpoint(result.state)
                    fail(result.error.toSaveMessage(uploadFailed = result.state.isAtUpload(newItem)))
                }
            }
        }
    }

    private fun fail(message: NativeText) {
        _uiState.update { it.copy(save = SaveStatus.Failed(message, isPartial = checkpoint.itemId != null)) }
    }

    /** Keeps [state] here and, once the item exists, in saved state. */
    private fun keepCheckpoint(state: AddItemState) {
        checkpoint = state
        if (state.itemId != null) savedStateHandle[SAVE_KEY] = json.encodeToString(SaveRecord.of(state))
    }

    /** Throws when a photo cannot be prepared. */
    private suspend fun AddItemUiState.toNewItem(): NewItem {
        val images = photos.mapIndexed { index, photo ->
            NewImage(photoFiles.prepare(photo.uri, photo.rotation), "photo-$index.jpg", JPEG_MIME_TYPE)
        }
        return NewItem(
            collectionId = collectionId,
            name = name.trim(),
            textFields = fields.map { NewTextField(it.label, it.value) },
            mainImage = images.firstOrNull(),
            imageFields = photos.drop(1).zip(images.drop(1)) { photo, image -> NewImageField(photo.label, image) },
            tagIds = selectedTagIds.toList()
        )
    }

    override fun onCleared() {
        photoFiles.delete(_uiState.value.photos.map { it.uri })
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
}

/** What [AddItemViewModel] keeps across process death. */
@Serializable
private data class AddItemDraft(
    val name: String = "",
    val fields: List<FieldRow> = emptyList(),
    val photos: List<PhotoRow> = emptyList(),
    val selectedTagIds: Set<String> = emptySet()
)

private const val DRAFT_KEY = "add_item_draft"
private const val SAVE_KEY = "add_item_save"
private const val TAG = "AddItemViewModel"

/**
 * What [AddItemViewModel] keeps of a save across process death: the [AddItemState] fields once
 * the item exists, and [savedItemId] once the save is done.
 */
@Serializable
private data class SaveRecord(
    val itemId: String? = null,
    val textFieldsDone: Int = 0,
    val mainImageDone: Boolean = false,
    val imageFieldIds: List<String> = emptyList(),
    val imageFieldsUploaded: Int = 0,
    val tagsDone: Boolean = false,
    val savedItemId: String? = null
) {
    fun toState() = AddItemState(itemId, textFieldsDone, mainImageDone, imageFieldIds, imageFieldsUploaded, tagsDone)

    companion object {
        fun of(state: AddItemState) = SaveRecord(
            itemId = state.itemId,
            textFieldsDone = state.textFieldsDone,
            mainImageDone = state.mainImageDone,
            imageFieldIds = state.imageFieldIds,
            imageFieldsUploaded = state.imageFieldsUploaded,
            tagsDone = state.tagsDone
        )
    }
}

/**
 * True when the next call of [newItem] after this state is an image upload: the main image, or
 * the image of a field that already exists.
 */
private fun AddItemState.isAtUpload(newItem: NewItem): Boolean = itemId != null &&
    textFieldsDone == newItem.textFields.size &&
    ((newItem.mainImage != null && !mainImageDone) || imageFieldIds.size > imageFieldsUploaded)

/** An upload 500 usually means a full upload quota (plan §2), so that one is named. */
private fun Throwable.toSaveMessage(uploadFailed: Boolean): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.add_item_save_error_network)

    is KoillectionError.Server -> if (uploadFailed && status == HTTP_INTERNAL_ERROR) {
        NativeText.Resource(RString.add_item_save_error_quota)
    } else {
        NativeText.Arguments(RString.add_item_save_error_http, listOf(status))
    }

    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))

    is KoillectionError.Http -> NativeText.Arguments(RString.add_item_save_error_http, listOf(status))

    is KoillectionError.Unauthorized -> NativeText.Arguments(
        RString.add_item_save_error_http,
        listOf(HTTP_UNAUTHORIZED)
    )

    is KoillectionError.NotFound -> NativeText.Arguments(RString.add_item_save_error_http, listOf(HTTP_NOT_FOUND))

    is KoillectionError.Validation -> NativeText.Resource(RString.add_item_save_error_validation)

    else -> NativeText.Resource(RString.add_item_save_error_unexpected)
}

private const val HTTP_INTERNAL_ERROR = 500
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404

private val json = Json { ignoreUnknownKeys = true }

/** The next clockwise quarter turn: 0, 90, 180, 270, then 0 again. */
internal fun Rotation.next(): Rotation = Rotation.entries[(ordinal + 1) % Rotation.entries.size]

/** The server's tags and categories, read in parallel and grouped by [groupTags]. */
internal suspend fun TagsRepository.loadTagGroups(): Result<List<TagGroup>> = coroutineScope {
    val tags = async { getTags() }
    val categories = async { getTagCategories() }
    val tagsResult = tags.await()
    val categoriesResult = categories.await()
    val failure = tagsResult.exceptionOrNull() ?: categoriesResult.exceptionOrNull()
    if (failure != null) {
        Result.failure(failure)
    } else {
        Result.success(groupTags(tagsResult.getOrThrow(), categoriesResult.getOrThrow()))
    }
}

/**
 * One group per category that has tags, sorted by category label; tags without a category (or
 * with a category the list does not hold) go last. Tags in a group are sorted by label.
 */
internal fun groupTags(tags: List<Tag>, categories: List<TagCategory>): List<TagGroup> {
    val categoryById = categories.associateBy { it.id }
    val byCategory = tags.groupBy { tag -> tag.categoryId?.let { categoryById[it] } }
    val categorized = byCategory.mapNotNull { (category, list) ->
        category?.let { TagGroup(label = it.label.orEmpty(), tags = list.sortedByLabel()) }
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label.orEmpty() })
    val uncategorized = byCategory[null]?.let { TagGroup(label = null, tags = it.sortedByLabel()) }
    return categorized + listOfNotNull(uncategorized)
}

private fun List<Tag>.sortedByLabel(): List<Tag> = sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

/** The message of a failed [loadTagGroups]. */
internal fun Throwable.toTagsMessage(): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.add_item_error_network)
    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
    is KoillectionError.Server -> NativeText.Arguments(RString.add_item_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.add_item_error_http, listOf(status))
    else -> NativeText.Resource(RString.add_item_error_unexpected)
}
