package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_network
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_unexpected
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The text part of the add-item form: name, free label/value rows, and a pick from the server's
 * existing tags. The form cannot create a tag (plan §5 decision 7). The save, and with it the
 * collection id, comes in M3.6.
 */
@KoinViewModel
class AddItemViewModel(private val tagsRepository: TagsRepository) : ViewModel() {

    private var nextFieldId = 0

    private val _uiState = MutableStateFlow(
        AddItemUiState(
            onNameChange = ::changeName,
            onAddField = ::addField,
            onRemoveField = ::removeField,
            onLabelChange = ::changeLabel,
            onValueChange = ::changeValue,
            onTagToggle = ::toggleTag,
            onRetryTags = ::retryTags
        )
    )
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    init {
        loadTags()
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

    private fun loadTags() {
        viewModelScope.launch {
            val tags = async { tagsRepository.getTags() }
            val categories = async { tagsRepository.getTagCategories() }
            val tagsResult = tags.await()
            val categoriesResult = categories.await()
            val failure = tagsResult.exceptionOrNull() ?: categoriesResult.exceptionOrNull()
            _uiState.update { state ->
                if (failure != null) {
                    state.copy(tagGroups = emptyList(), isLoadingTags = false, tagsError = failure.toMessage())
                } else {
                    state.copy(
                        tagGroups = groupTags(tagsResult.getOrThrow(), categoriesResult.getOrThrow()),
                        isLoadingTags = false,
                        tagsError = NativeText.Empty
                    )
                }
            }
        }
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

private fun Throwable.toMessage(): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.add_item_error_network)
    is KoillectionError.Server -> NativeText.Arguments(RString.add_item_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.add_item_error_http, listOf(status))
    else -> NativeText.Resource(RString.add_item_error_unexpected)
}
