package com.grappim.koillectionmobile.feature.tags.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup

/**
 * The tags of the user, grouped by category (`groupTags`). A failed load or retry clears [groups] and sets [error].
 * [expandedGroups] holds the positions in [groups] whose tags show; a header is keyed by position because two
 * categories may share a label. A load starts with all groups closed, except a lone group, which starts open.
 */
data class TagListUiState(
    val groups: List<TagGroup> = emptyList(),
    val expandedGroups: Set<Int> = emptySet(),
    val onToggleGroup: (Int) -> Unit = {},
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
