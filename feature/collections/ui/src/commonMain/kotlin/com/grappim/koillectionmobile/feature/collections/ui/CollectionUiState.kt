package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.items.domain.Item

/**
 * One collection: its [title], its direct [children], then its [items]. A load fills all three or
 * none: a failed load or retry clears them and sets [error].
 */
data class CollectionUiState(
    val title: String = "",
    val children: List<Collection> = emptyList(),
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
