package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish

/**
 * One wish. A load sets [wish] or clears it: a failed load or retry sets [error].
 *
 * The delete: [onDelete] runs it after the user confirms. [isDeleting] is true while it runs.
 * [isDeleted] is set once the server deleted the wish: the screen then leaves. A failed delete
 * sets [deleteError]; [onDeleteErrorShown] clears it.
 */
data class WishUiState(
    val wish: Wish? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onDelete: () -> Unit = {},
    val onDeleteErrorShown: () -> Unit = {}
) {
    /** The delete is offered only for a wish on screen, and not twice at once. */
    val canDelete: Boolean get() = wish != null && !isLoading && !isDeleting && !isDeleted
}
