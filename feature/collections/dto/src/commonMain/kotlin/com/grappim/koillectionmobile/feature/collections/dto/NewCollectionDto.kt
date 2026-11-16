package com.grappim.koillectionmobile.feature.collections.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/collections` (`collection:write` group, Koillection 1.8.5). `title` is
 * the only field the server requires (`#[Assert\NotBlank]`); the server picks the colour
 * (`ColorListener`).
 *
 * - [parent] is an IRI (`/api/collections/<id>`), or null for a top-level collection. A null is
 *   left out of the body.
 */
@Serializable
data class NewCollectionDto(val title: String, val parent: String? = null)
