package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList

/** The choice lists of the user, sorted by name ignoring case. A failed load or retry clears [choiceLists] and sets [error]. */
data class ChoiceListListUiState(
    val choiceLists: List<ChoiceList> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
