package com.grappim.koillectionmobile.feature.inventories.domain

import kotlinx.datetime.LocalDate

/**
 * An inventory: a check list of items that the web UI made from some collections. [collections]
 * is a copy made at creation, so later item renames or deletes do not change it. [createdAt] is
 * the date part as the server wrote it, with no time zone conversion.
 */
data class Inventory(
    val id: String,
    val name: String,
    val createdAt: LocalDate,
    val collections: List<InventoryCollection>
)

/**
 * One collection of an inventory. [totalItems] and [checkedItems] are the server's counts, and
 * they include the counts of [children]. A parent collection that the user did not pick still
 * comes as a node, with no [items] of its own.
 */
data class InventoryCollection(
    val id: String,
    val title: String,
    val children: List<InventoryCollection>,
    val items: List<InventoryItem>,
    val totalItems: Int,
    val checkedItems: Int
)

data class InventoryItem(val id: String, val name: String, val checked: Boolean)
