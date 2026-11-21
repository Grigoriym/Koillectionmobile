package com.grappim.koillectionmobile.feature.wishlists.domain

/**
 * A wish in wishlist [wishlistId].
 *
 * [price] is free text: the server stores it as a string. [currency] is an ISO 4217 code
 * (`EUR`), checked by the server. [imageUrl] and [thumbnailUrl] are full URLs; the server sends
 * the image as the thumbnail when it made no thumbnail.
 */
data class Wish(
    val id: String,
    val name: String,
    val wishlistId: String,
    val url: String?,
    val price: String?,
    val currency: String?,
    val comment: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?
)

/** The text fields of a wish that an edit can change. Each one is a key of the `PATCH` body. */
enum class WishField {
    NAME,
    URL,
    PRICE,
    CURRENCY,
    COMMENT
}
