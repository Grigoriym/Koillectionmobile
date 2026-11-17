package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.uikit.NativeText

/**
 * Every item of the user, read once. A failed load or retry clears [results] and sets [error].
 *
 * [query] filters [results] by item name into [visibleResults]; [onQueryChange] sets it.
 */
data class ItemSearchUiState(
    val results: List<ItemSearchResult> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val query: String = "",
    val onQueryChange: (String) -> Unit = {},
    val onRetry: () -> Unit = {}
) {
    /** The [results] whose item name holds [query], ignoring case. A blank [query] keeps every result. */
    val visibleResults: List<ItemSearchResult>
        get() = query.trim().let { q ->
            if (q.isEmpty()) results else results.filter { it.name.contains(q, ignoreCase = true) }
        }
}

/**
 * One item in the search. [collectionTitle] is the title of the item's collection, or null when
 * the item has no collection or the collection list does not hold it.
 */
data class ItemSearchResult(val itemId: String, val name: String, val collectionTitle: String?)
