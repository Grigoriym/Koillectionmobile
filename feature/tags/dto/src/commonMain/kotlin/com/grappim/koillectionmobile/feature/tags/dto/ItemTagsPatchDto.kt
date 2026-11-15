package com.grappim.koillectionmobile.feature.tags.dto

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/items/{id}` that sets an item's tags (`item:write` group, Koillection
 * 1.8.5). [tags] is the full list of tag IRIs (`/api/tags/<id>`): the server replaces the item's
 * tags with it, so a tag left out is removed. An empty list removes every tag.
 */
@Serializable
data class ItemTagsPatchDto(val tags: List<String>)
