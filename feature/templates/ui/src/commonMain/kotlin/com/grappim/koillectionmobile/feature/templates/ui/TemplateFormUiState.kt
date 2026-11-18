package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import kotlinx.serialization.Serializable

/**
 * One field row of the template form. [id] is local to the form (a stable list key). [fieldId] is
 * the server id of an existing field; null for a new one. [choiceListId] is used only for a
 * `choice-list` [type].
 */
@Serializable
data class FormField(
    val id: Int,
    val fieldId: String?,
    val name: String,
    val type: String,
    val choiceListId: String? = null
)

/** The template form's values: the [name] and the [fields] in the order the user wants. */
@Serializable
data class TemplateDraft(val name: String, val fields: List<FormField>)

/** The save, once it has started. While it is set, the screen shows it instead of the form. */
sealed interface TemplateSaveStatus {
    /** [done] of [total] server calls are finished. */
    data class Sending(val done: Int, val total: Int) : TemplateSaveStatus

    /** The save stopped. [isPartial]: a call went through, so only a retry is offered, not a return to the form. */
    data class Failed(val message: NativeText, val isPartial: Boolean) : TemplateSaveStatus
}

/**
 * The template form, new or edit. Until the form can show, [draft] is null: [isLoading] is true,
 * or [error] is set. [original] is the template as loaded (the empty form for a new template).
 * Both survive process death. [choiceLists] fill the choice list picker.
 *
 * [savedTemplateId] is set once the save is done: the screen then leaves.
 */
data class TemplateFormUiState(
    val isNew: Boolean = true,
    val draft: TemplateDraft? = null,
    val original: TemplateDraft? = null,
    val choiceLists: List<ChoiceList> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val save: TemplateSaveStatus? = null,
    val savedTemplateId: String? = null,
    val onRetry: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onAddField: () -> Unit = {},
    val onRemoveField: (Int) -> Unit = {},
    val onFieldNameChange: (Int, String) -> Unit = { _, _ -> },
    val onFieldTypeChange: (Int, String) -> Unit = { _, _ -> },
    val onFieldChoiceListChange: (Int, String) -> Unit = { _, _ -> },
    val onMoveUp: (Int) -> Unit = {},
    val onMoveDown: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The user changed something, so leaving the form asks first. */
    val hasChanges: Boolean get() = draft != null && draft != original

    /**
     * The field names that more than one field uses, compared trimmed, as the save sends them.
     * The server takes a duplicate (plan §2, M8.2), so the form blocks it. A blank name is left out.
     */
    val duplicateNames: Set<String>
        get() = draft?.fields.orEmpty()
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    /**
     * A name, a non-blank unique name per field, a choice list for each `choice-list` field, and,
     * on the edit form, a change.
     */
    val canSave: Boolean
        get() {
            val draft = draft ?: return false
            return draft.name.isNotBlank() &&
                draft.fields.none { it.name.isBlank() } &&
                draft.fields.none { it.type == TYPE_CHOICE_LIST && it.choiceListId == null } &&
                duplicateNames.isEmpty() &&
                (isNew || hasChanges)
        }
}
