package com.grappim.koillectionmobile.feature.collections.dto

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/collections/{id}` (`collection:write` group, Koillection 1.8.5). A merge
 * patch: a null property is left out of the body, so the server keeps that value.
 *
 * - [deleteImage] `true` makes the server remove the image and its file (`#[Upload(deleteProperty:
 *   'deleteImage')]`, `UploadListener`).
 */
@Serializable
data class CollectionPatchDto(val title: String? = null, val deleteImage: Boolean? = null)

/**
 * The body of `PATCH /api/collections/{id}` that sets the collection's default template
 * (`itemsDefaultTemplate`, `collection:write`). [itemsDefaultTemplate] is a template IRI
 * (`/api/templates/<id>`), or null to clear it. It has no default value, so a null is sent as an
 * explicit `null`: a merge patch without the key keeps the stored template (plan §2, M8.2).
 */
@Serializable
data class CollectionDefaultTemplatePatchDto(val itemsDefaultTemplate: String?)
