package com.grappim.koillectionmobile.feature.collections.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a collection screen that is still on the back stack that its collection changed in this
 * process (the edit form saved it), so it reads the collection again. One instance per app (bound
 * in `feature:collections:data`). A screen that is not collecting misses the signal; it reads the
 * collection when it starts anyway.
 */
class CollectionChanges {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** The ids of collections that changed. */
    val changedCollections: SharedFlow<String> = changes.asSharedFlow()

    fun notifyChanged(collectionId: String) {
        changes.tryEmit(collectionId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
