package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One image of the edit form: the main image, or an image field. [datumId] is the server id of an
 * existing image field; null for the main image and for a new image field. [previewUrl] is the
 * server's image (null when there is none, or for a new photo). [uri] is a photo on the device
 * that replaces the server's image, or the photo of a new image field; [rotation] applies to it.
 * [id] is local to the form, as in [FieldRow]. The main image's [label] is not used.
 */
@Serializable
data class EditPhoto(
    val id: Int,
    val datumId: String? = null,
    val previewUrl: String? = null,
    val uri: String? = null,
    val rotation: Rotation = Rotation.NONE,
    val label: String = ""
)

/** One non-image field of the edit form. [id] is local to the form, as in [FieldRow]. */
@Serializable
sealed interface EditRow {
    val id: Int
    val label: String

    /** A `text` field. [datumId] is the server id; null for a new field. */
    @Serializable
    @SerialName("text")
    data class Text(override val id: Int, val datumId: String?, override val label: String, val value: String) :
        EditRow

    /** A field of a type the form cannot edit: shown, never changed. [datumType] and [value] as the server sent them. */
    @Serializable
    @SerialName("read_only")
    data class ReadOnly(override val id: Int, override val label: String, val datumType: String, val value: String?) :
        EditRow
}

/** What the edit form holds: the values as loaded, and the user's edits of them. */
@Serializable
data class EditItemDraft(
    val name: String,
    val mainImage: EditPhoto,
    val photos: List<EditPhoto>,
    val fields: List<EditRow>,
    val selectedTagIds: Set<String>
)

/**
 * The edit form of an item. Until the item is loaded, [draft] is null: [isLoading] is true, or
 * [error] is set. [original] is the item as loaded. Both survive process death.
 *
 * While [save] is set, the screen shows it instead of the form. [isSaved] is set once the save is
 * done: the screen then leaves.
 *
 * [draft] holds the [EditItemDraft.mainImage] (replace only), the image fields in
 * [EditItemDraft.photos] (existing first, then new ones), and the text and read-only fields in
 * [EditItemDraft.fields] (existing in server order, then new ones).
 */
data class EditItemUiState(
    val draft: EditItemDraft? = null,
    val original: EditItemDraft? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val tagGroups: List<TagGroup> = emptyList(),
    val isLoadingTags: Boolean = true,
    val tagsError: NativeText = NativeText.Empty,
    val save: SaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onAddField: () -> Unit = {},
    val onRemoveField: (Int) -> Unit = {},
    val onLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onValueChange: (Int, String) -> Unit = { _, _ -> },
    val onTagToggle: (String) -> Unit = {},
    val onRetryTags: () -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoReplace: (Int, List<String>) -> Unit = { _, _ -> },
    val onPhotoRotate: (Int) -> Unit = {},
    val onPhotoRemove: (Int) -> Unit = {},
    val onPhotoLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The user changed something, so leaving the form asks first. */
    val hasChanges: Boolean get() = draft != null && draft != original

    /**
     * The labels that more than one field uses: text, read-only and image fields together, as in
     * [AddItemUiState.duplicateLabels]. A blank label is left out.
     */
    val duplicateLabels: Set<String>
        get() {
            val draft = draft ?: return emptySet()
            return (draft.fields.map { it.label } + draft.photos.map { it.label })
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        }

    /** The server rejects a blank label (`NotBlank`). A read-only field is not checked: the form does not send it. */
    val hasBlankLabel: Boolean
        get() {
            val draft = draft ?: return false
            val blankText = draft.fields.any { it is EditRow.Text && it.label.isBlank() }
            return blankText || draft.photos.any { it.label.isBlank() }
        }

    /** As on the add form: a name, and unique, non-blank labels. */
    val canSave: Boolean
        get() = draft != null && draft.name.isNotBlank() && duplicateLabels.isEmpty() && !hasBlankLabel
}
