package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList

/** The server's choice lists, for the `choice-list` rows. As the tags: [error] is set when the load failed. */
data class ChoiceListsUi(
    val lists: List<ChoiceList> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty
)
