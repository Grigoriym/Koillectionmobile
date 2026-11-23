package com.grappim.koillectionmobile.feature.albums.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a screen that is still on the back stack that a photo changed in this process (a form
 * created or edited it, or a delete removed it), so it reads again. One instance per app (bound in `feature:albums:data`). A screen
 * that is not collecting misses the signal; it reads when it starts anyway.
 */
class PhotoChanges {
    private val changes = MutableSharedFlow<PhotoChange>(extraBufferCapacity = BUFFER)

    val changed: SharedFlow<PhotoChange> = changes.asSharedFlow()

    fun notifyChanged(photoId: String, albumId: String) {
        changes.tryEmit(PhotoChange(photoId, albumId))
    }

    private companion object {
        const val BUFFER = 8
    }
}

/** Photo [photoId] in album [albumId] changed: the screen of that photo and of that album read again. */
data class PhotoChange(val photoId: String, val albumId: String)
