package com.grappim.koillectionmobile.feature.albums.dto

import kotlinx.serialization.Serializable

/**
 * An Album as `GET /api/albums`, `/api/albums/{id}` and `/api/albums/{id}/children` send it
 * (`album:read` group, Koillection 1.8.5). Only the fields the app reads; the client's `Json`
 * ignores the rest.
 *
 * - [parent] is an IRI (`/api/albums/<id>`), not an object: the entity marks it
 *   `readableLink: false`.
 * - [image] is a path relative to the server root (`uploads/<user>/<file>.jpg`), or null.
 * - [color] is a hex colour without `#`.
 */
@Serializable
data class AlbumDto(
    val id: String,
    val title: String,
    val parent: String? = null,
    val color: String? = null,
    val image: String? = null
)

/**
 * The body of `POST /api/albums` (`album:write` group). `title` is the only field the server
 * requires (`#[Assert\NotBlank]`); the server picks the colour.
 *
 * - [parent] is an IRI (`/api/albums/<id>`), or null for a top-level album. A null is left out of
 *   the body.
 */
@Serializable
data class NewAlbumDto(val title: String, val parent: String? = null)

/**
 * The body of `PATCH /api/albums/{id}` (`album:write` group). A merge patch: a null property is
 * left out of the body, so the server keeps that value.
 *
 * - [deleteImage] `true` makes the server remove the image and its file (`#[Upload(deleteProperty:
 *   'deleteImage')]`).
 */
@Serializable
data class AlbumPatchDto(val title: String? = null, val deleteImage: Boolean? = null)
