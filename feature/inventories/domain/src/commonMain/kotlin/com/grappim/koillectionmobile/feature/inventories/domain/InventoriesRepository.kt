package com.grappim.koillectionmobile.feature.inventories.domain

/**
 * Access to the inventories of the user. The API reads and deletes only: the web UI makes an
 * inventory and ticks its items. A failure is a `KoillectionError` (`core:domain`) inside the
 * [Result].
 */
interface InventoriesRepository {

    /** All inventories, from all pages, in the server's order. */
    suspend fun getInventories(): Result<List<Inventory>>

    suspend fun getInventory(id: String): Result<Inventory>

    suspend fun deleteInventory(id: String): Result<Unit>
}
