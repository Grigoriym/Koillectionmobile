package com.grappim.koillectionmobile.feature.albums.domain

import kotlinx.datetime.LocalDate

/**
 * A photo in album [albumId].
 *
 * [imageUrl] and [thumbnailUrl] are full URLs; the server sends the image as the thumbnail when it
 * made no thumbnail. [takenAt] is the date part of the server's date-time, or null. The app shows
 * it and does not edit it: the API cannot set it (plan §5 decision 14).
 */
data class Photo(
    val id: String,
    val title: String,
    val albumId: String,
    val comment: String?,
    val place: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val takenAt: LocalDate?
)

/** The text fields of a photo that an edit can change. Each one is a key of the `PATCH` body. */
enum class PhotoField {
    TITLE,
    COMMENT,
    PLACE
}
