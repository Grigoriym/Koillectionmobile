package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.items.domain.Item

/**
 * One collection: its [title], its direct [children], then its [items]. A load fills all three or
 * none: a failed load or retry clears them and sets [error].
 *
 * The delete has two steps. [onDeleteClick] counts what the server deletes with the collection;
 * [isCountingDelete] is true while it runs. The count then fills [deleteSummary], and the screen
 * shows the confirm. [onDeleteConfirm] runs the delete, [onDeleteDismiss] drops the confirm.
 * [isDeleting] is true while the delete runs. [isDeleted] is set once the server deleted the
 * collection: the screen then leaves. A failed count or delete sets [deleteError];
 * [onDeleteErrorShown] clears it.
 */
data class CollectionUiState(
    val title: String = "",
    val children: List<Collection> = emptyList(),
    val items: List<Item> = emptyList(),
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
    /** The delete is offered only for a collection on screen, and not twice at once. */
    val canDelete: Boolean
        get() = !isLoading && error.isEmpty() && !isCountingDelete && deleteSummary == null && !isDeleting &&
            !isDeleted
}

/**
 * What the server deletes with a collection: [childCount] collections inside it (every level),
 * and [itemCount] items in it and in those collections.
 */
data class DeleteSummary(val childCount: Int, val itemCount: Int)
