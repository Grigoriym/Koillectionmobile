package com.grappim.koillectionmobile.feature.items.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/items` (`item:write` group, Koillection 1.8.5). `name` and `collection`
 * are the two fields the server requires (`#[Assert\NotBlank]`).
 *
 * - [collection] is an IRI (`/api/collections/<id>`), as `add_book.py` sends it.
 */
@Serializable
data class NewItemDto(val name: String, val collection: String)
