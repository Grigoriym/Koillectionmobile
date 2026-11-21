package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The routes this module owns. Each one is registered in the shell's `navKeySerializersModule`:
 * a route missing from there only breaks back-stack restore after process death.
 */
@Serializable
data object WishlistListRoute : NavKey

/** The id is all it carries. The screen reads the wishlist again, not a snapshot of the list. */
@Serializable
data class WishlistRoute(val wishlistId: String) : NavKey

/**
 * The new-wishlist form. [parentId] is the wishlist the new one goes into, or null for the top
 * level.
 */
@Serializable
data class NewWishlistRoute(val parentId: String? = null) : NavKey

/** The edit form of wishlist [wishlistId]. */
@Serializable
data class EditWishlistRoute(val wishlistId: String) : NavKey
