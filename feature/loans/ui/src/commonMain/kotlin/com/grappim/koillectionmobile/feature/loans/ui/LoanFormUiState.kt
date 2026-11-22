package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import kotlinx.datetime.LocalDate

/** The save, once it has started. While it is set, the screen shows it instead of the form. */
sealed interface LoanSaveStatus {
    data object Sending : LoanSaveStatus

    /** The one call failed, so nothing is on the server: a retry or a return to the form. */
    data class Failed(val message: NativeText) : LoanSaveStatus
}

/**
 * The loan form. [isEdit] is false for a new loan. [original] is what the form started from: the
 * loan as the server sent it, or for a new loan an empty name and today. A new loan has no returned
 * date on the form, as the web form (`LoanType`) has none.
 *
 * While [isLoading] or with an [error] (edit form only), the form is not shown. [isSaved] is set
 * once the loan is on the server: the screen then leaves.
 */
data class LoanFormUiState(
    val isEdit: Boolean = false,
    val original: LoanFormValues? = null,
    val values: LoanFormValues? = null,
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val save: LoanSaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onLentToChange: (String) -> Unit = {},
    val onLentAtChange: (LocalDate) -> Unit = {},
    val onReturnedAtChange: (LocalDate?) -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** What an edit save sends. For a new loan: what the user changed on the form. */
    val changes: List<LoanChange>
        get() {
            val start = original ?: return emptyList()
            return values?.changesFrom(start).orEmpty()
        }

    /** The user changed something, so leaving the form asks first. */
    val hasInput: Boolean
        get() = changes.isNotEmpty()

    /** A blank name is blocked. An edit without a change sends nothing. */
    val canSave: Boolean
        get() = values?.canSave == true && (!isEdit || changes.isNotEmpty())
}
