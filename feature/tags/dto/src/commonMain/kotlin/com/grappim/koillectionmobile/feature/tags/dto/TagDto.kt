package com.grappim.koillectionmobile.feature.tags.dto

import kotlinx.serialization.Serializable

/**
 * A Tag as `GET /api/tags` and `/api/items/{id}/tags` send it (`tag:read` group, Koillection
 * 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [category] is an IRI (`/api/tag_categories/<id>`), not an object, or null.
 */
@Serializable
data class TagDto(val id: String, val label: String, val description: String? = null, val category: String? = null)
