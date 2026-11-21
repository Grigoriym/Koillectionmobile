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
