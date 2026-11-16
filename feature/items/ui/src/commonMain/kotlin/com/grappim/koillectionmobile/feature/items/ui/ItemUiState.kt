package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.items.domain.Image

/**
 * One item: its [name], main [image], [fields] in `position` order and [tags] (labels). A load
 * fills all of them or none: a failed load or retry clears them and sets [error].
 *
 * The delete: [onDelete] runs it after the user confirms. [isDeleting] is true while it runs.
 * [isDeleted] is set once the server deleted the item: the screen then leaves. A failed delete
 * sets [deleteError]; [onDeleteErrorShown] clears it.
 */
data class ItemUiState(
    val name: String = "",
    val image: Image? = null,
    val fields: List<ItemField> = emptyList(),
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onDelete: () -> Unit = {},
    val onDeleteErrorShown: () -> Unit = {}
) {
    /** The delete is offered only for an item on screen, and not twice at once. */
    val canDelete: Boolean get() = !isLoading && error.isEmpty() && !isDeleting && !isDeleted

    /** The edit is offered only for an item on screen that is not being deleted. */
    val canEdit: Boolean get() = canDelete
}
