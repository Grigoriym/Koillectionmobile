package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.tags.domain.Tag

/** One free label/value row. [id] is local to the form: a key for the list, not a server id. */
data class FieldRow(val id: Int, val label: String = "", val value: String = "")

/** The tags of one category, sorted by label. [label] is null for tags without a category. */
data class TagGroup(val label: String?, val tags: List<Tag>)

/**
 * The add-item form: the [name], the free [fields] in order, and the tags picked from the
 * server's [tagGroups]. [tagsError] is set when the tag load failed; the rest of the form still works.
 */
data class AddItemUiState(
    val name: String = "",
    val fields: List<FieldRow> = emptyList(),
    val tagGroups: List<TagGroup> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val isLoadingTags: Boolean = true,
    val tagsError: NativeText = NativeText.Empty,
    val onNameChange: (String) -> Unit = {},
    val onAddField: () -> Unit = {},
    val onRemoveField: (Int) -> Unit = {},
    val onLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onValueChange: (Int, String) -> Unit = { _, _ -> },
    val onTagToggle: (String) -> Unit = {},
    val onRetryTags: () -> Unit = {}
) {
    /**
     * The labels that more than one row uses. The server compares labels exactly
     * (`DatumLabelNotExistsInParentValidator`: `===`), so this does too: "Author" and "author"
     * are two labels. A blank label is left out: the server rejects it for another reason
     * (`NotBlank`).
     */
    val duplicateLabels: Set<String>
        get() = fields.map { it.label }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
}
