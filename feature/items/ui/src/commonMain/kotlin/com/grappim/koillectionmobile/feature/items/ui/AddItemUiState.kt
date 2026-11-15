package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import kotlinx.serialization.Serializable

/** One free label/value row. [id] is local to the form: a key for the list, not a server id. */
@Serializable
data class FieldRow(val id: Int, val label: String = "", val value: String = "")

/**
 * One photo: a content [uri], the user's extra [rotation] on top of the EXIF orientation, and the
 * image field's [label]. The first photo of the form is the main image, and its label is not used.
 * [id] is local to the form, as in [FieldRow].
 */
@Serializable
data class PhotoRow(val id: Int, val uri: String, val rotation: Rotation = Rotation.NONE, val label: String = "")

/** The tags of one category, sorted by label. [label] is null for tags without a category. */
data class TagGroup(val label: String?, val tags: List<Tag>)

/** The save, once it has started. While it is set, the screen shows it instead of the form. */
sealed interface SaveStatus {
    /** The photos are being turned into upload bytes. */
    data object Preparing : SaveStatus

    /** [done] of [total] server calls are finished. */
    data class Sending(val done: Int, val total: Int) : SaveStatus

    /**
     * The save stopped. [isPartial]: the item exists on the server, so only a retry (which sends
     * the rest) is offered, not a return to the form.
     */
    data class Failed(val message: NativeText, val isPartial: Boolean) : SaveStatus
}

/**
 * The add-item form: the [name], the free [fields] in order, the [photos] (the first is the main
 * image, the rest are image fields in order), and the tags picked from the server's [tagGroups].
 * [tagsError] is set when the tag load failed; the rest of the form still works.
 * [savedItemId] is set once the item is saved: the screen then opens it.
 */
data class AddItemUiState(
    val name: String = "",
    val fields: List<FieldRow> = emptyList(),
    val photos: List<PhotoRow> = emptyList(),
    val tagGroups: List<TagGroup> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val isLoadingTags: Boolean = true,
    val tagsError: NativeText = NativeText.Empty,
    val save: SaveStatus? = null,
    val savedItemId: String? = null,
    val onNameChange: (String) -> Unit = {},
    val onAddField: () -> Unit = {},
    val onRemoveField: (Int) -> Unit = {},
    val onLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onValueChange: (Int, String) -> Unit = { _, _ -> },
    val onTagToggle: (String) -> Unit = {},
    val onRetryTags: () -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: (Int) -> Unit = {},
    val onPhotoRemove: (Int) -> Unit = {},
    val onPhotoMakeMain: (Int) -> Unit = {},
    val onPhotoLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /**
     * The labels that more than one row uses, text rows and image fields together: both are data
     * of one item. The server compares labels exactly (`DatumLabelNotExistsInParentValidator`:
     * `===`), so this does too: "Author" and "author" are two labels. A blank label is left out:
     * the server rejects it for another reason (`NotBlank`). The main image has no label.
     */
    val duplicateLabels: Set<String>
        get() = (fields.map { it.label } + photos.drop(1).map { it.label })
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    /** The server rejects a blank label (`NotBlank`), so a text row or image field without one blocks the save. */
    val hasBlankLabel: Boolean
        get() = fields.any { it.label.isBlank() } || photos.drop(1).any { it.label.isBlank() }

    /** The server needs a name (`NotBlank`) and unique, non-blank labels. */
    val canSave: Boolean
        get() = name.isNotBlank() && duplicateLabels.isEmpty() && !hasBlankLabel
}
