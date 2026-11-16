package com.grappim.koillectionmobile.feature.collections.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a screen that is still on the back stack that a list of collections changed in this
 * process, so it reads the list again. One instance per app (bound in `feature:collections:data`).
 * A screen that is not collecting misses the signal; it reads the list when it starts anyway.
 */
class CollectionListChanges {
    private val changes = MutableSharedFlow<String?>(extraBufferCapacity = BUFFER)

    /**
     * The parent of each changed collection: a collection id for a child list, or null for the
     * top-level list.
     */
    val changedParents: SharedFlow<String?> = changes.asSharedFlow()

    fun notifyChanged(parentId: String?) {
        changes.tryEmit(parentId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
