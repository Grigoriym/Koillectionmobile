package com.grappim.koillectionmobile.feature.inventories.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory

/**
 * The inventories, newest first. A failed load or retry clears [inventories] and sets [error].
 *
 * [onDeleteClick] fills [deleteConfirm], and the screen asks first; [onDeleteConfirm] runs the
 * delete, [onDeleteDismiss] drops the question. [deletingId] is the inventory whose delete runs;
 * while it is set, the screen starts no other delete. On success the list reads again. A failure
 * keeps [inventories] and sets [deleteError]; [onDeleteErrorShown] clears it.
 */
data class InventoryListUiState(
    val inventories: List<Inventory> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val deletingId: String? = null,
    val deleteConfirm: Inventory? = null,
    val deleteError: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val onDeleteClick: (String) -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onDeleteErrorShown: () -> Unit = {}
)

/** The checked and total items of [this], from the server's counts of the root collections. */
internal val Inventory.checkedItems: Int get() = collections.sumOf { it.checkedItems }

internal val Inventory.totalItems: Int get() = collections.sumOf { it.totalItems }
