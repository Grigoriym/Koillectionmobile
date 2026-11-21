package com.grappim.koillectionmobile.feature.wishlists.domain

/**
 * A Koillection wishlist. Wishlists nest, as collections do: a top-level one has no [parentId].
 *
 * [imageUrl] is a full URL. The server sends a path relative to its root, and the mapper resolves
 * it against the stored server URL. [color] is a hex colour without `#`, picked by the server.
 */
data class Wishlist(val id: String, val name: String, val parentId: String?, val color: String?, val imageUrl: String?)
