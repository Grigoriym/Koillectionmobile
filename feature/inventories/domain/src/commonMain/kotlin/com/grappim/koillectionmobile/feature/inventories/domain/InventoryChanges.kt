package com.grappim.koillectionmobile.feature.inventories.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells the inventory list, while it waits lower on the back stack, that the inventory screen
 * deleted an inventory in this process, so the list reads again. One instance per app (bound in
 * `feature:inventories:data`). A list that is not collecting misses the signal; it reads when it
 * starts anyway.
 */
class InventoryChanges {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** The id of the deleted inventory. */
    val deleted: SharedFlow<String> = changes.asSharedFlow()

    fun notifyDeleted(inventoryId: String) {
        changes.tryEmit(inventoryId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
