package com.grappim.koillectionmobile.feature.collections.dto

import kotlinx.serialization.Serializable

/**
 * A Collection as `GET /api/collections`, `/api/collections/{id}` and
 * `/api/collections/{id}/children` send it (`collection:read` group, Koillection 1.8.5). Only the
 * fields the app reads; the client's `Json` ignores the rest.
 *
 * - [parent] is an IRI (`/api/collections/<id>`), not an object: the entity marks it
 *   `readableLink: false`.
 * - [image] is a path relative to the server root (`uploads/<user>/<file>.jpg`), or null.
 * - [color] is a hex colour without `#` (`EEEEEE`).
 * - [itemsDefaultTemplate] is the IRI of the template the add form applies (`/api/templates/<id>`),
 *   or null.
 */
@Serializable
data class CollectionDto(
    val id: String,
    val title: String,
    val parent: String? = null,
    val color: String? = null,
    val image: String? = null,
    val itemsDefaultTemplate: String? = null
)
