package com.grappim.koillectionmobile.feature.albums.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a screen that is still on the back stack that an album changed in this process (a form
 * saved it), so it reads again. One instance per app (bound in `feature:albums:data`). A screen
 * that is not collecting misses the signal; it reads when it starts anyway.
 */
class AlbumChanges {
    private val changes = MutableSharedFlow<AlbumChange>(extraBufferCapacity = BUFFER)

    val changed: SharedFlow<AlbumChange> = changes.asSharedFlow()

    fun notifyChanged(albumId: String, parentId: String?) {
        changes.tryEmit(AlbumChange(albumId, parentId))
    }

    private companion object {
        const val BUFFER = 8
    }
}

/**
 * Album [albumId] changed. [parentId] is its parent, or null for a top-level album: the screen that
 * lists it reads again.
 */
data class AlbumChange(val albumId: String, val parentId: String?)
