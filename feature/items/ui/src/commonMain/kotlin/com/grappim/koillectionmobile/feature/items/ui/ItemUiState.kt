package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.items.domain.Image

/**
 * One item: its [name], main [image], [fields] in `position` order and [tags] (labels). A load
 * fills all of them or none: a failed load or retry clears them and sets [error].
 */
data class ItemUiState(
    val name: String = "",
    val image: Image? = null,
    val fields: List<ItemField> = emptyList(),
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
