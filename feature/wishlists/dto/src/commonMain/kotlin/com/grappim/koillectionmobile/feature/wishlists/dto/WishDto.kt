package com.grappim.koillectionmobile.feature.wishlists.dto

import kotlinx.serialization.Serializable

/**
 * A Wish as `GET /api/wishes/{id}` and `/api/wishlists/{id}/wishes` send it (`wish:read` group,
 * Koillection 1.8.5). Only the fields the app reads.
 *
 * - [wishlist] is an IRI (`/api/wishlists/<id>`).
 * - [price] is a string: the column is a string.
 * - [image] and [imageSmallThumbnail] are paths relative to the server root, or null. The server
 *   sends [image] as the thumbnail when it has no thumbnail.
 */
@Serializable
data class WishDto(
    val id: String,
    val name: String,
    val wishlist: String,
    val url: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val comment: String? = null,
    val image: String? = null,
    val imageSmallThumbnail: String? = null
)

/**
 * The body of `POST /api/wishes` (`wish:write` group). [name] and [wishlist] (an IRI,
 * `/api/wishlists/<id>`) are required (`#[Assert\NotBlank]`). A null optional field is left out
 * of the body.
 */
@Serializable
data class NewWishDto(
    val name: String,
    val wishlist: String,
    val url: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val comment: String? = null
)

/**
 * The keys of `PATCH /api/wishes/{id}` (`wish:write` group). The repository builds the body as a
 * JSON object from only the changed keys, so a cleared field can be an explicit `null`.
 */
object WishPatchKeys {
    const val NAME = "name"
    const val URL = "url"
    const val PRICE = "price"
    const val CURRENCY = "currency"
    const val COMMENT = "comment"
}
