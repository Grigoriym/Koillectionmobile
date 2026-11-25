package com.grappim.koillectionmobile.feature.inventories.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryCollection
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryItem

/**
 * One inventory: its [name] and its tree of [collections], in the server's order. A failed load or
 * retry clears both and sets [error].
 *
 * [expandedIds] holds the ids of the collections whose items and children show. A load starts with
 * all collections closed, except a lone root collection, which starts open. [onToggle] opens or
 * closes one collection. [rows] is the tree as the screen shows it.
 *
 * [onDeleteClick] sets [isDeleteConfirm], and the screen asks first; [onDeleteConfirm] runs the
 * delete, [onDeleteDismiss] drops the question. [isDeleting] is true while the delete runs.
 * [isDeleted] is set once the server deleted the inventory: the screen then leaves. A failure sets
 * [deleteError]; [onDeleteErrorShown] clears it.
 */
data class InventoryUiState(
    val name: String = "",
    val collections: List<InventoryCollection> = emptyList(),
    val expandedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val isDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val onToggle: (String) -> Unit = {},
    val onDeleteClick: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onDeleteErrorShown: () -> Unit = {}
) {
    /** The delete is offered only for an inventory on screen, and not twice at once. */
    val canDelete: Boolean
        get() = !isLoading && error.isEmpty() && !isDeleteConfirm && !isDeleting && !isDeleted

    /**
     * The visible rows, depth first. An open collection shows its items, then its children, as the
     * web UI does (`Inventory/show.html.twig`).
     */
    val rows: List<InventoryRow>
        get() = buildList { collections.forEach { addRows(it, depth = 0) } }

    private fun MutableList<InventoryRow>.addRows(collection: InventoryCollection, depth: Int) {
        val isExpanded = collection.id in expandedIds
        add(InventoryRow.Collection(collection, depth, isExpanded))
        if (!isExpanded) return
        collection.items.forEach { add(InventoryRow.Item(it, collection.id, depth + 1)) }
        collection.children.forEach { addRows(it, depth + 1) }
    }
}

/** One line of the inventory screen. [depth] is 0 for a root collection. */
sealed interface InventoryRow {
    val key: String
    val depth: Int

    data class Collection(val collection: InventoryCollection, override val depth: Int, val isExpanded: Boolean) :
        InventoryRow {
        override val key: String get() = "collection:${collection.id}"
    }

    /** [collectionId] is the collection the item is in; it makes the key unique across the tree. */
    data class Item(val item: InventoryItem, val collectionId: String, override val depth: Int) : InventoryRow {
        override val key: String get() = "item:$collectionId:${item.id}"
    }
}
