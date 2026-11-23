package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.collections.domain.Collection

/**
 * [isLoading] is the first load only. A pull to refresh sets [isRefreshing] and keeps the list on
 * the screen. A failed refresh keeps the old list and sets [error].
 */
data class CollectionListUiState(
    val collections: List<Collection> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onRefresh: () -> Unit = {}
)
