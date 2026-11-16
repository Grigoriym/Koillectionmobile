package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.item_error_unexpected
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
 * replaced, not removed. The save is not here yet.
 *
 * The item is read once. The loaded values and the edits are kept in [savedStateHandle], so after
 * a process death the form shows the edits and does not read the item again. The tag list is read
 * again.
 *
 * Photos taken or picked here are cache files ([PhotoFiles]). They are deleted when a photo is
 * removed or replaced again, and when the form leaves the back stack ([onCleared]).
 */
@KoinViewModel
class EditItemViewModel(
    @InjectedParam private val itemId: String,
    private val itemsRepository: ItemsRepository,
    private val tagsRepository: TagsRepository,
    private val photoFiles: PhotoFiles,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: SavedForm? = savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<SavedForm>(it) }

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
            onPhotoLabelChange = ::changePhotoLabel
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

private val json = Json { ignoreUnknownKeys = true }
