package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.loans.domain.Loan

/**
 * The loans of one item: the loans that are not returned first, then by lent date, newest first.
 * A failed load or retry clears [loans] and sets [error].
 *
 * The actions on one loan: [onMarkReturned] sets today as its returned date. [onDeleteClick]
 * fills [deleteConfirm], and the section asks first; [onDeleteConfirm] runs the delete,
 * [onDeleteDismiss] drops the question. [busyLoanId] is the loan whose action runs; while it is
 * set, the section starts no other action. On success the section reads again. A failure keeps
 * [loans] and sets [actionError]; [onActionErrorShown] clears it.
 */
data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val busyLoanId: String? = null,
    val deleteConfirm: Loan? = null,
    val actionError: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val onMarkReturned: (String) -> Unit = {},
    val onDeleteClick: (String) -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onActionErrorShown: () -> Unit = {}
)
