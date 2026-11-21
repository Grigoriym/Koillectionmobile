package com.grappim.koillectionmobile.feature.wishlists.dto

import kotlinx.serialization.Serializable

/**
 * A Wishlist as `GET /api/wishlists`, `/api/wishlists/{id}` and `/api/wishlists/{id}/children`
 * send it (`wishlist:read` group, Koillection 1.8.5). Only the fields the app reads; the client's
 * `Json` ignores the rest.
 *
 * - [parent] is an IRI (`/api/wishlists/<id>`), not an object: the entity marks it
 *   `readableLink: false`.
 * - [image] is a path relative to the server root (`uploads/<user>/<file>.jpg`), or null.
 * - [color] is a hex colour without `#`.
 */
@Serializable
data class WishlistDto(
    val id: String,
    val name: String,
    val parent: String? = null,
    val color: String? = null,
    val image: String? = null
)

/**
 * The body of `POST /api/wishlists` (`wishlist:write` group). `name` is the only field the server
 * requires (`#[Assert\NotBlank]`); the server picks the colour.
 *
 * - [parent] is an IRI (`/api/wishlists/<id>`), or null for a top-level wishlist. A null is left
 *   out of the body.
 */
@Serializable
data class NewWishlistDto(val name: String, val parent: String? = null)

/**
 * The body of `PATCH /api/wishlists/{id}` (`wishlist:write` group). A merge patch: a null
 * property is left out of the body, so the server keeps that value.
 *
 * - [deleteImage] `true` makes the server remove the image and its file (`#[Upload(deleteProperty:
 *   'deleteImage')]`).
 */
@Serializable
data class WishlistPatchDto(val name: String? = null, val deleteImage: Boolean? = null)
