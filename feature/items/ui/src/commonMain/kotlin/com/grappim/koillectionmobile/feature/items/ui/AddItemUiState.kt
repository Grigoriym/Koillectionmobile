package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHOICE_LIST
import com.grappim.koillectionmobile.feature.items.domain.TYPE_TEXT
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import kotlinx.serialization.Serializable

/**
 * One label/value row. [id] is local to the form: a key for the list, not a server id. [type] is
 * one of `TEXT_TYPES`; [value] is in the server's format for it (see `FieldInput.kt`). A
 * `choice-list` row needs [choiceListId] before the save.
 */
@Serializable
data class FieldRow(
    val id: Int,
    val label: String = "",
    val value: String = "",
    val type: String = TYPE_TEXT,
    val choiceListId: String? = null
)

/**
 * One photo: a content [uri], the user's extra [rotation] on top of the EXIF orientation, and the
 * image field's [label]. The first photo of the form is the main image, and its label is not used.
 * [id] is local to the form, as in [FieldRow].
 */
@Serializable
data class PhotoRow(val id: Int, val uri: String, val rotation: Rotation = Rotation.NONE, val label: String = "")

/**
 * An image field a template added (M8.7): a [label] and, once the user took or picked one, a photo
 * ([uri], [rotation]). A slot is never the main image. A slot without a photo sends nothing: the
 * save drops it (user, 2026-11-19). [id] is local to the form, as in [FieldRow].
 */
@Serializable
data class ImageSlot(val id: Int, val label: String, val uri: String? = null, val rotation: Rotation = Rotation.NONE)

/**
 * The template picker, while it is open. [isLoading] covers the template list and, after a pick,
 * the template's fields. [error] is set when either load failed; a retry repeats that load.
 */
data class TemplatePickerUi(
    val templates: List<Template> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty
)

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
 * image, the rest are image fields in order), the [imageSlots] a template added, the tags picked from the server's [tagGroups], and
 * the [newTagLabels] of tags the save creates (see [withNewTagLabel]).
 * [tagsError] is set when the tag load failed; the rest of the form still works. [choiceLists]
 * serve the `choice-list` rows. [templatePicker] is set while the template picker is open;
 * [skippedTemplateFields] are the fields of the last applied template that the form cannot fill.
 * [savedItemId] is set once the item is saved: the screen then opens it.
 */
data class AddItemUiState(
    val name: String = "",
    val fields: List<FieldRow> = emptyList(),
    val photos: List<PhotoRow> = emptyList(),
    val imageSlots: List<ImageSlot> = emptyList(),
    val tagGroups: List<TagGroup> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val newTagLabels: List<String> = emptyList(),
    val isLoadingTags: Boolean = true,
    val tagsError: NativeText = NativeText.Empty,
    val choiceLists: ChoiceListsUi = ChoiceListsUi(),
    val templatePicker: TemplatePickerUi? = null,
    val skippedTemplateFields: List<TemplateField> = emptyList(),
    val save: SaveStatus? = null,
    val savedItemId: String? = null,
    val onNameChange: (String) -> Unit = {},
    val onAddField: () -> Unit = {},
    val onRemoveField: (Int) -> Unit = {},
    val onLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onValueChange: (Int, String) -> Unit = { _, _ -> },
    val onTypeChange: (Int, String) -> Unit = { _, _ -> },
    val onChoiceListChange: (Int, String) -> Unit = { _, _ -> },
    val onCheckedChange: (Int, Boolean) -> Unit = { _, _ -> },
    val onChoiceToggle: (Int, String) -> Unit = { _, _ -> },
    val onRetryChoiceLists: () -> Unit = {},
    val onTagToggle: (String) -> Unit = {},
    val onNewTag: (String) -> Unit = {},
    val onNewTagRemove: (String) -> Unit = {},
    val onRetryTags: () -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: (Int) -> Unit = {},
    val onPhotoRemove: (Int) -> Unit = {},
    val onPhotoMakeMain: (Int) -> Unit = {},
    val onPhotoLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onSlotPhotos: (Int, List<String>) -> Unit = { _, _ -> },
    val onSlotRotate: (Int) -> Unit = {},
    val onSlotRemove: (Int) -> Unit = {},
    val onSlotLabelChange: (Int, String) -> Unit = { _, _ -> },
    val onOpenTemplates: () -> Unit = {},
    val onCloseTemplates: () -> Unit = {},
    val onTemplatePick: (String) -> Unit = {},
    val onRetryTemplates: () -> Unit = {},
    val onSkippedDismiss: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The image slots with a photo: the save sends these and drops the others. */
    val filledSlots: List<ImageSlot>
        get() = imageSlots.filter { it.uri != null }

    /**
     * The labels that more than one row uses, text rows, image fields and filled image slots
     * together: all are data of one item. The server compares labels exactly (`DatumLabelNotExistsInParentValidator`:
     * `===`), so this does too: "Author" and "author" are two labels. A blank label is left out:
     * the server rejects it for another reason (`NotBlank`). The main image has no label.
     */
    val duplicateLabels: Set<String>
        get() = (fields.map { it.label } + photos.drop(1).map { it.label } + filledSlots.map { it.label })
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    /** The server rejects a blank label (`NotBlank`), so a text row or image field without one blocks the save. */
    val hasBlankLabel: Boolean
        get() = fields.any { it.label.isBlank() } || photos.drop(1).any { it.label.isBlank() } ||
            filledSlots.any { it.label.isBlank() }

    /** The user entered something, so leaving the form asks first. */
    val hasInput: Boolean
        get() = name.isNotBlank() || fields.isNotEmpty() || photos.isNotEmpty() || imageSlots.isNotEmpty() ||
            selectedTagIds.isNotEmpty() || newTagLabels.isNotEmpty()

    /** The server needs a choice list for a `choice-list` field, and rejects some values (see [isValueInvalid]). */
    val hasInvalidField: Boolean
        get() = fields.any {
            (it.type == TYPE_CHOICE_LIST && it.choiceListId == null) ||
                isValueInvalid(it.type, it.value)
        }

    /** The server needs a name (`NotBlank`), unique, non-blank labels, and valid fields. */
    val canSave: Boolean
        get() = name.isNotBlank() && duplicateLabels.isEmpty() && !hasBlankLabel && !hasInvalidField
}
