package com.grappim.koillectionmobile.feature.wishlists.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a screen that is still on the back stack that a wish changed in this process (a delete
 * removed it), so it reads again. One instance per app (bound in `feature:wishlists:data`). A screen
 * that is not collecting misses the signal; it reads when it starts anyway.
 */
class WishChanges {
    private val changes = MutableSharedFlow<WishChange>(extraBufferCapacity = BUFFER)

    val changed: SharedFlow<WishChange> = changes.asSharedFlow()

    fun notifyChanged(wishId: String, wishlistId: String) {
        changes.tryEmit(WishChange(wishId, wishlistId))
    }

    private companion object {
        const val BUFFER = 8
    }
}

/** Wish [wishId] in wishlist [wishlistId] changed: the screen of that wishlist reads again. */
data class WishChange(val wishId: String, val wishlistId: String)
