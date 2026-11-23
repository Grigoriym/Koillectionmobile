package com.grappim.koillectionmobile.feature.albums.dto

import kotlinx.serialization.Serializable

/**
 * A Photo as `GET /api/photos/{id}` and `/api/albums/{id}/photos` send it (`photo:read` group,
 * Koillection 1.8.5). Only the fields the app reads.
 *
 * - [album] is an IRI (`/api/albums/<id>`).
 * - [image] and [imageSmallThumbnail] are paths relative to the server root, or null. The server
 *   sends [image] as the thumbnail when it has no thumbnail.
 * - [takenAt] is an ISO 8601 date-time with an offset, or null. It is in `photo:read` only: the API
 *   cannot set it.
 */
@Serializable
data class PhotoDto(
    val id: String,
    val title: String,
    val album: String,
    val comment: String? = null,
    val place: String? = null,
    val image: String? = null,
    val imageSmallThumbnail: String? = null,
    val takenAt: String? = null
)

/**
 * The body of `POST /api/photos` (`photo:write` group). [title] and [album] (an IRI,
 * `/api/albums/<id>`) are required (`#[Assert\NotBlank]`). A null optional field is left out of
 * the body.
 */
@Serializable
data class NewPhotoDto(val title: String, val album: String, val comment: String? = null, val place: String? = null)

/**
 * The keys of `PATCH /api/photos/{id}` (`photo:write` group). The repository builds the body as a
 * JSON object from only the changed keys, so a cleared field can be an explicit `null`.
 */
object PhotoPatchKeys {
    const val TITLE = "title"
    const val COMMENT = "comment"
    const val PLACE = "place"
}
