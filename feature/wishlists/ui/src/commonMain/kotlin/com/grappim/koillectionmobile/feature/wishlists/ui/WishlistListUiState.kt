package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist

/**
 * The top-level wishlists, sorted by name ignoring case. A failed load or retry clears
 * [wishlists] and sets [error].
 */
data class WishlistListUiState(
    val wishlists: List<Wishlist> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
