package com.grappim.koillectionmobile.feature.items.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/data` (`datum:write` group, Koillection 1.8.5), for a field of an item.
 *
 * - [item] is an IRI (`/api/items/<id>`).
 * - [type] is one of `DatumTypeEnum::TYPES`; the app sends `text` or `image`.
 * - [value] must be null for the file types (`image`, `file`, `video`, `sign`): the server checks
 *   it with `Assert\IsNull`. A null [value] is left out of the body, as `add_book.py` does. The
 *   image itself goes up later, to `POST /api/data/{id}/image`.
 */
@Serializable
data class NewDatumDto(
    val item: String,
    val type: String,
    val label: String,
    val position: Int,
    val value: String? = null
)
