package com.grappim.koillectionmobile.feature.tags.dto

import kotlinx.serialization.Serializable

/**
 * A TagCategory as `GET /api/tag_categories` sends it (`tagCategory:read` group, Koillection
 * 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [label] is nullable in the entity (`TagCategory::$label`).
 * - [color] is a hex value without `#` (`F3E5F5`).
 */
@Serializable
data class TagCategoryDto(
    val id: String,
    val label: String? = null,
    val description: String? = null,
    val color: String? = null
)
