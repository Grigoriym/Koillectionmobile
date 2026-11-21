package com.grappim.koillectionmobile.feature.wishlists.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a screen that is still on the back stack that a wishlist changed in this process (a form
 * saved it), so it reads again. One instance per app (bound in `feature:wishlists:data`). A screen
 * that is not collecting misses the signal; it reads when it starts anyway.
 */
class WishlistChanges {
    private val changes = MutableSharedFlow<WishlistChange>(extraBufferCapacity = BUFFER)

    val changed: SharedFlow<WishlistChange> = changes.asSharedFlow()

    fun notifyChanged(wishlistId: String, parentId: String?) {
        changes.tryEmit(WishlistChange(wishlistId, parentId))
    }

    private companion object {
        const val BUFFER = 8
    }
}

/**
 * Wishlist [wishlistId] changed. [parentId] is its parent, or null for a top-level wishlist: the
 * screen that lists it reads again.
 */
data class WishlistChange(val wishlistId: String, val parentId: String?)
