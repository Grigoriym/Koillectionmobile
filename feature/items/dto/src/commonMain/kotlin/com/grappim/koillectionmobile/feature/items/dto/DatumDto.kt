package com.grappim.koillectionmobile.feature.items.dto

import kotlinx.serialization.Serializable

/**
 * A Datum (one field of an item) as `GET /api/items/{id}/data` sends it (`datum:read` group,
 * Koillection 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [type] is one of `DatumTypeEnum::TYPES` in the Koillection source: `text`, `textarea`,
 *   `image`, `date`, `rating` and others.
 * - [value] is null for the file types (`image`, `file`, `video`, `sign`).
 * - [image] and its thumbnails are paths relative to the server root, as on [ItemDto].
 * - [choiceList] is an IRI (`/api/choice_lists/<id>`), set on a `choice-list` field (M8.2).
 */
@Serializable
data class DatumDto(
    val id: String,
    val type: String,
    val label: String? = null,
    val value: String? = null,
    val position: Int? = null,
    val image: String? = null,
    val imageSmallThumbnail: String? = null,
    val imageLargeThumbnail: String? = null,
    val choiceList: String? = null
)
