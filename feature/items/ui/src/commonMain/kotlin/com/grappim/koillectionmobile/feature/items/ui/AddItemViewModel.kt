package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.logger.logcat
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.photos.JPEG_MIME_TYPE
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.core.photos.next
import com.grappim.koillectionmobile.feature.items.domain.AddItemResult
import com.grappim.koillectionmobile.feature.items.domain.AddItemState
import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.NewImage
import com.grappim.koillectionmobile.feature.items.domain.NewImageField
import com.grappim.koillectionmobile.feature.items.domain.NewItem
import com.grappim.koillectionmobile.feature.items.domain.NewTextField
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHOICE_LIST
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.tags.domain.loadTagGroups
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
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
import com.grappim.koillectionmobile.strings.generated.resources.add_item_templates_error
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
 * The add-item form: name, typed label/value rows, photos, a pick from the server's existing tags,
 * and labels of new tags. A row's type picks its input (`FieldInput.kt`); a `choice-list` row
 * picks from the server's choice lists, which load with the tags. A template adds its fields as
 * empty rows and image slots ([applyTemplate]). A new tag is created by the save, not when it is added, so a form left
 * without saving creates no tag. The save creates the item in [collectionId] through
 * [AddItemUseCase].
 *
 * What the user typed and picked is kept in [savedStateHandle], so it survives process death. The
 * tags and the choice lists are loaded again. A started save keeps what exists on the server there too, so a retry
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
    private val templatesRepository: TemplatesRepository,
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
    private var nextSlotId = (draft.imageSlots.maxOfOrNull { it.id } ?: -1) + 1

    /** The template whose fields the picker loads, so a retry loads them again. Null: the list loads. */
    private var pickedTemplateId: String? = null

    private val _uiState = MutableStateFlow(
        AddItemUiState(
            name = draft.name,
            fields = draft.fields,
            photos = draft.photos,
            imageSlots = draft.imageSlots,
            selectedTagIds = draft.selectedTagIds,
            newTagLabels = draft.newTagLabels,
            onNameChange = ::changeName,
            onAddField = ::addField,
            onRemoveField = ::removeField,
            onLabelChange = ::changeLabel,
            onValueChange = ::changeValue,
            onTypeChange = ::changeType,
            onChoiceListChange = ::changeChoiceList,
            onCheckedChange = ::changeChecked,
            onChoiceToggle = ::toggleChoice,
            onRetryChoiceLists = ::retryChoiceLists,
            onTagToggle = ::toggleTag,
            onNewTag = ::addNewTag,
            onNewTagRemove = ::removeNewTag,
            onRetryTags = ::retryTags,
            onPhotosAdd = ::addPhotos,
            onPhotoRotate = ::rotatePhoto,
            onPhotoRemove = ::removePhoto,
            onPhotoMakeMain = ::makeMain,
            onPhotoLabelChange = ::changePhotoLabel,
            onSlotPhotos = ::fillSlot,
            onSlotRotate = ::rotateSlot,
            onSlotRemove = ::removeSlot,
            onSlotLabelChange = ::changeSlotLabel,
            onOpenTemplates = ::openTemplates,
            onCloseTemplates = ::closeTemplates,
            onTemplatePick = ::pickTemplate,
            onRetryTemplates = ::retryTemplates,
            onSkippedDismiss = ::dismissSkipped,
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
                .map { AddItemDraft(it.name, it.fields, it.photos, it.imageSlots, it.selectedTagIds, it.newTagLabels) }
                .distinctUntilChanged()
                .collect { savedStateHandle[DRAFT_KEY] = json.encodeToString(it) }
        }
        loadTags()
        loadChoiceLists()
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

    /** The first of [uris] goes into slot [id]; a second pick has no slot, so its copy is deleted. */
    private fun fillSlot(id: Int, uris: List<String>) {
        val slot = _uiState.value.imageSlots.firstOrNull { it.id == id && it.uri == null }
        val uri = uris.firstOrNull()
        if (slot == null || uri == null) {
            photoFiles.delete(uris)
            return
        }
        updateSlot(id) { it.copy(uri = uri) }
        photoFiles.delete(uris.drop(1))
    }

    private fun rotateSlot(id: Int) = updateSlot(id) { it.copy(rotation = it.rotation.next()) }

    private fun removeSlot(id: Int) {
        val removed = _uiState.value.imageSlots.filter { it.id == id }
        _uiState.update { s -> s.copy(imageSlots = s.imageSlots.filterNot { it.id == id }) }
        photoFiles.delete(removed.mapNotNull { it.uri })
    }

    private fun changeSlotLabel(id: Int, label: String) = updateSlot(id) { it.copy(label = label) }

    private fun updateSlot(id: Int, change: (ImageSlot) -> ImageSlot) {
        _uiState.update { s -> s.copy(imageSlots = s.imageSlots.map { if (it.id == id) change(it) else it }) }
    }

    private fun openTemplates() {
        if (_uiState.value.templatePicker != null) return
        pickedTemplateId = null
        _uiState.update { it.copy(templatePicker = TemplatePickerUi()) }
        loadTemplates()
    }

    private fun closeTemplates() {
        pickedTemplateId = null
        _uiState.update { it.copy(templatePicker = null) }
    }

    private fun pickTemplate(templateId: String) {
        val picker = _uiState.value.templatePicker ?: return
        if (picker.isLoading) return
        pickedTemplateId = templateId
        _uiState.update { it.copy(templatePicker = picker.copy(isLoading = true, error = NativeText.Empty)) }
        loadTemplateFields(templateId)
    }

    private fun retryTemplates() {
        val picker = _uiState.value.templatePicker ?: return
        if (picker.isLoading) return
        _uiState.update { it.copy(templatePicker = picker.copy(isLoading = true, error = NativeText.Empty)) }
        pickedTemplateId?.let(::loadTemplateFields) ?: loadTemplates()
    }

    /** Sorted by name, ignoring case, as the template list. */
    private fun loadTemplates() {
        viewModelScope.launch {
            val result = templatesRepository.getTemplates()
            updatePicker { picker ->
                result.fold(
                    onSuccess = { templates ->
                        picker.copy(
                            templates = templates.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                            isLoading = false
                        )
                    },
                    onFailure = { picker.copy(isLoading = false, error = templatesError) }
                )
            }
        }
    }

    private fun loadTemplateFields(templateId: String) {
        viewModelScope.launch {
            val result = templatesRepository.getFields(templateId)
            // The picker closed while the fields loaded: the user does not want them any more.
            if (_uiState.value.templatePicker == null || pickedTemplateId != templateId) return@launch
            result.fold(
                onSuccess = { fields ->
                    pickedTemplateId = null
                    applyTemplateFields(fields)
                },
                onFailure = { updatePicker { it.copy(isLoading = false, error = templatesError) } }
            )
        }
    }

    /** Adds [fields] after the rows the form has, closes the picker, and names the skipped fields. */
    private fun applyTemplateFields(fields: List<TemplateField>) {
        _uiState.update { s ->
            val usedLabels =
                s.fields.map { it.label } + s.photos.drop(1).map { it.label } + s.imageSlots.map { it.label }
            val applied = applyTemplate(fields, usedLabels.toSet(), { nextFieldId++ }, { nextSlotId++ })
            s.copy(
                fields = s.fields + applied.rows,
                imageSlots = s.imageSlots + applied.slots,
                templatePicker = null,
                skippedTemplateFields = applied.skipped
            )
        }
    }

    private fun dismissSkipped() {
        _uiState.update { it.copy(skippedTemplateFields = emptyList()) }
    }

    /** Changes the picker only while it is open: a load that ends after a close changes nothing. */
    private fun updatePicker(change: (TemplatePickerUi) -> TemplatePickerUi) {
        _uiState.update { s -> s.templatePicker?.let { s.copy(templatePicker = change(it)) } ?: s }
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

    private fun changeType(id: Int, type: String) = updateField(id) {
        it.copy(
            type = type,
            value = valueAfterTypeChange(it.value, it.type, type),
            choiceListId = if (type == TYPE_CHOICE_LIST) it.choiceListId else null
        )
    }

    /** A new choice list has other choices: the picks of the old one go. */
    private fun changeChoiceList(id: Int, choiceListId: String) = updateField(id) {
        if (it.choiceListId == choiceListId) it else it.copy(choiceListId = choiceListId, value = emptyValue(it.type))
    }

    private fun changeChecked(id: Int, isChecked: Boolean) = updateField(id) {
        it.copy(value = checkboxValue(isChecked))
    }

    private fun toggleChoice(id: Int, choice: String) {
        val lists = _uiState.value.choiceLists.lists
        updateField(id) { row ->
            val choices = lists.firstOrNull { it.id == row.choiceListId }?.choices.orEmpty()
            row.copy(value = withChoiceToggled(row.value, choice, choices))
        }
    }

    private fun updateField(id: Int, change: (FieldRow) -> FieldRow) {
        _uiState.update { s -> s.copy(fields = s.fields.map { if (it.id == id) change(it) else it }) }
    }

    private fun toggleTag(tagId: String) {
        _uiState.update { s ->
            val selected = s.selectedTagIds
            s.copy(selectedTagIds = if (tagId in selected) selected - tagId else selected + tagId)
        }
    }

    private fun addNewTag(label: String) {
        _uiState.update { s ->
            val choice = TagChoice(s.selectedTagIds, s.newTagLabels).withNewTagLabel(label, s.tagGroups)
            s.copy(selectedTagIds = choice.selectedTagIds, newTagLabels = choice.newTagLabels)
        }
    }

    private fun removeNewTag(label: String) {
        _uiState.update { s -> s.copy(newTagLabels = s.newTagLabels - label) }
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
                    photoFiles.delete(form.photos.map { it.uri } + form.imageSlots.mapNotNull { it.uri })
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
        val slotFields = filledSlots.mapIndexed { index, slot ->
            val image = NewImage(
                photoFiles.prepare(checkNotNull(slot.uri), slot.rotation),
                "slot-$index.jpg",
                JPEG_MIME_TYPE
            )
            NewImageField(slot.label, image)
        }
        return NewItem(
            collectionId = collectionId,
            name = name.trim(),
            textFields = fields.map { NewTextField(it.label, it.value, it.type, it.choiceListId) },
            mainImage = images.firstOrNull(),
            imageFields = photos.drop(1).zip(images.drop(1)) { photo, image -> NewImageField(photo.label, image) } +
                slotFields,
            tagIds = selectedTagIds.toList(),
            newTagLabels = newTagLabels
        )
    }

    override fun onCleared() {
        photoFiles.delete(_uiState.value.photos.map { it.uri } + _uiState.value.imageSlots.mapNotNull { it.uri })
    }

    private fun retryChoiceLists() {
        if (_uiState.value.choiceLists.isLoading) return
        _uiState.update { it.copy(choiceLists = ChoiceListsUi()) }
        loadChoiceLists()
    }

    private fun loadChoiceLists() {
        viewModelScope.launch {
            val choiceLists = templatesRepository.loadChoiceLists()
            _uiState.update { it.copy(choiceLists = choiceLists) }
        }
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
    val imageSlots: List<ImageSlot> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val newTagLabels: List<String> = emptyList()
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
internal fun Throwable.toSaveMessage(uploadFailed: Boolean): NativeText = when (this) {
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

private val templatesError = NativeText.Resource(RString.add_item_templates_error)

/** The message of a failed `loadTagGroups`. */
internal fun Throwable.toTagsMessage(): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.add_item_error_network)
    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
    is KoillectionError.Server -> NativeText.Arguments(RString.add_item_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.add_item_error_http, listOf(status))
    else -> NativeText.Resource(RString.add_item_error_unexpected)
}
