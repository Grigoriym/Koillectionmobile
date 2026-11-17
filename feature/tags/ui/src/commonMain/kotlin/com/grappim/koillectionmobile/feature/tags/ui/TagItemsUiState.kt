package com.grappim.koillectionmobile.feature.tags.ui

import com.grappim.kit.uikit.NativeText

/** The items of one tag, in the server's order. A failed load or retry clears [items] and sets [error]. */
data class TagItemsUiState(
    val items: List<TagItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)

/** One item of the tag: what a row shows, and the id it opens. */
data class TagItem(val itemId: String, val name: String)
