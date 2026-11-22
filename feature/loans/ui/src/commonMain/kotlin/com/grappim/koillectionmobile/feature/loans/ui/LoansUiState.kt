package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.loans.domain.Loan

/**
 * The loans of one item: the loans that are not returned first, then by lent date, newest first.
 * A failed load or retry clears [loans] and sets [error].
 */
data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
