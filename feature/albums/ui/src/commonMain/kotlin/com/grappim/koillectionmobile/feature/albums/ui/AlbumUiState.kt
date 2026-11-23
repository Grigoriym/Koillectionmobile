package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.Photo

/**
 * One album: its [title], its direct [children], then its [photos], each sorted by title ignoring
 * case. A load fills all three or none: a failed load or retry clears them and sets [error].
 *
 * The delete has two steps. [onDeleteClick] counts what the server deletes with the album;
 * [isCountingDelete] is true while it runs. The count then fills [deleteSummary], and the screen
 * shows the confirm. [onDeleteConfirm] runs the delete, [onDeleteDismiss] drops the confirm.
 * [isDeleting] is true while the delete runs. [isDeleted] is set once the server deleted the
 * album: the screen then leaves. A failed count or delete sets [deleteError]; [onDeleteErrorShown]
 * clears it.
 */
data class AlbumUiState(
    val title: String = "",
    val children: List<Album> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val isCountingDelete: Boolean = false,
    val deleteSummary: DeleteSummary? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onDeleteClick: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onDeleteErrorShown: () -> Unit = {}
) {
    /** The delete is offered only for an album on screen, and not twice at once. */
    val canDelete: Boolean
        get() = !isLoading && error.isEmpty() && !isCountingDelete && deleteSummary == null && !isDeleting &&
            !isDeleted
}

/**
 * What the server deletes with an album: [childCount] albums inside it (every level), and
 * [photoCount] photos in it and in those albums.
 */
data class DeleteSummary(val childCount: Int, val photoCount: Int)
