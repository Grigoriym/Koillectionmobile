package com.grappim.koillectionmobile.feature.items.dto

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/items/{id}` that renames an item (`item:write` group, Koillection
 * 1.8.5). A merge patch: the server changes only [name].
 */
@Serializable
data class ItemNamePatchDto(val name: String)
