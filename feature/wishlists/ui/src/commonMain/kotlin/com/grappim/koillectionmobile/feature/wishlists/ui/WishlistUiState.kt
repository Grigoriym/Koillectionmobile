package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist

/**
 * One wishlist: its [name], its direct [children], then its [wishes], each sorted by name ignoring
 * case. A load fills all three or none: a failed load or retry clears them and sets [error].
 */
data class WishlistUiState(
    val name: String = "",
    val children: List<Wishlist> = emptyList(),
    val wishes: List<Wish> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
