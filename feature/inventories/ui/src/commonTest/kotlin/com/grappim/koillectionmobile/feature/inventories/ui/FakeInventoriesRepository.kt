package com.grappim.koillectionmobile.feature.inventories.ui

import com.grappim.koillectionmobile.feature.inventories.domain.InventoriesRepository
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryCollection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/**
 * Each list read takes the first queued answer in [lists]; each one-inventory read takes the first
 * queued answer in [ones]; each delete takes the first queued answer in [deletes]. Queue one
 * before the call.
 */
class FakeInventoriesRepository : InventoriesRepository {
    val lists = ArrayDeque<CompletableDeferred<Result<List<Inventory>>>>()
    var listCalls = 0
    val ones = ArrayDeque<CompletableDeferred<Result<Inventory>>>()
    val oneCalls = mutableListOf<String>()
    val deletes = ArrayDeque<CompletableDeferred<Result<Unit>>>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun getInventories(): Result<List<Inventory>> {
        listCalls++
        return lists.first().await()
    }

    override suspend fun getInventory(id: String): Result<Inventory> {
        oneCalls += id
        return ones.first().await()
    }

    override suspend fun deleteInventory(id: String): Result<Unit> {
        deleteCalls += id
        return deletes.first().await()
    }
}

internal fun inventory(id: String, name: String, createdAt: LocalDate, roots: List<InventoryCollection> = emptyList()) =
    Inventory(id = id, name = name, createdAt = createdAt, collections = roots)

internal fun root(id: String, totalItems: Int, checkedItems: Int) = InventoryCollection(
    id = id,
    title = "Collection $id",
    children = emptyList(),
    items = emptyList(),
    totalItems = totalItems,
    checkedItems = checkedItems
)
