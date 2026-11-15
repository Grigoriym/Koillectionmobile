package com.grappim.koillectionmobile.feature.items.dto

import kotlinx.serialization.Serializable

/**
 * An Item as `GET /api/items/{id}` and `/api/collections/{id}/items` send it (`item:read` group,
 * Koillection 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [collection] is an IRI (`/api/collections/<id>`), not an object.
 * - [image] and its thumbnails are paths relative to the server root (`uploads/<user>/<file>.jpg`),
 *   or null.
 */
@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val quantity: Int = 1,
    val collection: String? = null,
    val image: String? = null,
    val imageSmallThumbnail: String? = null,
    val imageLargeThumbnail: String? = null
)
