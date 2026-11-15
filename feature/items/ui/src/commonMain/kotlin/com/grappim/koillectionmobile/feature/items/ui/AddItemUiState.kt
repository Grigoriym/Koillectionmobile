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

/**
 * The add-item form: the [name], the free [fields] in order, the [photos] (the first is the main
 * image, the rest are image fields in order), and the tags picked from the server's [tagGroups].
 * [tagsError] is set when the tag load failed; the rest of the form still works.
 */
data class AddItemUiState(
    val name: String = "",
    val fields: List<FieldRow> = emptyList(),
    val photos: List<PhotoRow> = emptyList(),
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
    val onRetryTags: () -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: (Int) -> Unit = {},
    val onPhotoRemove: (Int) -> Unit = {},
    val onPhotoMakeMain: (Int) -> Unit = {},
    val onPhotoLabelChange: (Int, String) -> Unit = { _, _ -> }
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
}
