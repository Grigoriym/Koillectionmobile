package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.uikit.NativeText
import kotlinx.serialization.Serializable

/** One choice row of the choice list form. [id] is local to the form (a stable list key). */
@Serializable
data class FormChoice(val id: Int, val value: String)

/** The choice list form's values: the [name] and the [choices] in the order the user wants. */
@Serializable
data class ChoiceListDraft(val name: String, val choices: List<FormChoice>)

/**
 * The choice list form, new or edit. Until the form can show, [draft] is null: [isLoading] is
 * true, or [error] is set. [original] is the choice list as loaded (the empty form for a new one).
 * Both survive process death.
 *
 * [isSaving] and [isDeleting] are true while that call runs. [actionError] is the message of a
 * failed save or delete. [isDone] is set once the save or the delete went through: the screen
 * then leaves.
 */
data class ChoiceListFormUiState(
    val isNew: Boolean = true,
    val draft: ChoiceListDraft? = null,
    val original: ChoiceListDraft? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val actionError: NativeText = NativeText.Empty,
    val isDone: Boolean = false,
    val onRetry: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onAddChoice: () -> Unit = {},
    val onRemoveChoice: (Int) -> Unit = {},
    val onChoiceChange: (Int, String) -> Unit = { _, _ -> },
    val onMoveUp: (Int) -> Unit = {},
    val onMoveDown: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onActionErrorDismiss: () -> Unit = {}
) {
    val isBusy: Boolean get() = isSaving || isDeleting || isDone

    /** The user changed something. */
    val hasChanges: Boolean get() = draft != null && draft != original

    /**
     * The choices that more than one row has, compared trimmed, as the save sends them. The server
     * rejects a duplicate (`ChoiceList::$choices` is `Assert\Unique`). A blank choice is left out.
     */
    val duplicateChoices: Set<String>
        get() = draft?.choices.orEmpty()
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    /** A name, non-blank unique choices, no call running, and, on the edit form, a change. */
    val canSave: Boolean
        get() {
            val draft = draft ?: return false
            return draft.name.isNotBlank() &&
                draft.choices.none { it.value.isBlank() } &&
                duplicateChoices.isEmpty() &&
                !isBusy &&
                (isNew || hasChanges)
        }

    /** The edit form is loaded and no call runs. */
    val canDelete: Boolean get() = !isNew && draft != null && !isBusy
}
