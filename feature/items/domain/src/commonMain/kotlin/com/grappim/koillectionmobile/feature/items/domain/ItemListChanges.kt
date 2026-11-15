package com.grappim.koillectionmobile.feature.items.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a collection screen that is still on the back stack that its item list changed in this
 * process, so it reads the list again. One instance per app (bound in `feature:items:data`).
 * A screen that is not collecting misses the signal; it reads the list when it starts anyway.
 */
class ItemListChanges {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** The ids of collections whose item list changed. */
    val changedCollections: SharedFlow<String> = changes.asSharedFlow()

    fun notifyChanged(collectionId: String) {
        changes.tryEmit(collectionId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
