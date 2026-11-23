package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.albums.domain.Photo

/**
 * One photo. A load sets [photo] or clears it: a failed load or retry sets [error].
 *
 * The delete: [onDelete] runs it after the user confirms. [isDeleting] is true while it runs.
 * [isDeleted] is set once the server deleted the photo: the screen then leaves. A failed delete
 * sets [deleteError]; [onDeleteErrorShown] clears it.
 */
data class PhotoUiState(
    val photo: Photo? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onDelete: () -> Unit = {},
    val onDeleteErrorShown: () -> Unit = {}
) {
    /** The delete is offered only for a photo on screen, and not twice at once. */
    val canDelete: Boolean get() = photo != null && !isLoading && !isDeleting && !isDeleted
}
