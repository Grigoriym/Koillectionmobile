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
