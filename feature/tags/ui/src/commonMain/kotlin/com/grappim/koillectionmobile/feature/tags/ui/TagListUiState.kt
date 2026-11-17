package com.grappim.koillectionmobile.feature.tags.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup

/** The tags of the user, grouped by category (`groupTags`). A failed load or retry clears [groups] and sets [error]. */
data class TagListUiState(
    val groups: List<TagGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
