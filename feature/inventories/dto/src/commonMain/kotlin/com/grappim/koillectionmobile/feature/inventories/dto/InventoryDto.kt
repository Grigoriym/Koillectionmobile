package com.grappim.koillectionmobile.feature.inventories.dto

import kotlinx.serialization.Serializable

/**
 * An Inventory as `GET /api/inventories/{id}` and the list send it (`inventory:read` group,
 * Koillection 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [content] is the JSON tree that `InventoryHandler::buildInventory` built: the root
 *   collections. The entity column is nullable, so a missing key is an empty list.
 * - [createdAt] is a date-time (`DATETIME_IMMUTABLE`), as the server formats it.
 */
@Serializable
data class InventoryDto(
    val id: String,
    val name: String,
    val content: List<InventoryCollectionDto> = emptyList(),
    val createdAt: String
)

/**
 * One collection node of `content`. `InventoryHandler::buildCollection` adds [children] to a
 * parent node only through an append, so a missing [children] or [items] key is an empty list.
 */
@Serializable
data class InventoryCollectionDto(
    val id: String,
    val title: String,
    val children: List<InventoryCollectionDto> = emptyList(),
    val items: List<InventoryItemDto> = emptyList(),
    val totalItems: Int,
    val totalCheckedItems: Int
)

/** [checked] is a JSON boolean. */
@Serializable
data class InventoryItemDto(val id: String, val name: String, val checked: Boolean)
