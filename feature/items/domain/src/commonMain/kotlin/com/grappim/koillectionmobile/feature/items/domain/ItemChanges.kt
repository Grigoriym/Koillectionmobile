package com.grappim.koillectionmobile.feature.items.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells an item screen that is still on the back stack that its item changed in this process (the
 * edit form saved it), so it reads the item again. One instance per app (bound in
 * `feature:items:data`). A screen that is not collecting misses the signal; it reads the item when
 * it starts anyway.
 */
class ItemChanges {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** The ids of items that changed. */
    val changedItems: SharedFlow<String> = changes.asSharedFlow()

    fun notifyChanged(itemId: String) {
        changes.tryEmit(itemId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
