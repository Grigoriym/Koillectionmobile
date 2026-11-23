package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import kotlinx.serialization.Serializable

/**
 * The text fields of the photo form, as the user typed them. An empty field is no value: the server
 * gets null for it ([valueOf]).
 */
@Serializable
data class PhotoFormValues(val title: String = "", val comment: String = "", val place: String = "") {

    /** The server needs a title (`NotBlank` in `Photo.php`). */
    val hasTitle: Boolean
        get() = title.isNotBlank()

    fun text(field: PhotoField): String = when (field) {
        PhotoField.TITLE -> title
        PhotoField.COMMENT -> comment
        PhotoField.PLACE -> place
    }

    fun with(field: PhotoField, text: String): PhotoFormValues = when (field) {
        PhotoField.TITLE -> copy(title = text)
        PhotoField.COMMENT -> copy(comment = text)
        PhotoField.PLACE -> copy(place = text)
    }

    /** The value of [field] as the server gets it: trimmed, or null when empty. */
    fun valueOf(field: PhotoField): String? = text(field).trim().ifEmpty { null }

    /**
     * The fields whose value differs from [original], with the new value. A null value clears the
     * field: the server takes an explicit `null` (M11.2, plan §2).
     */
    fun changesFrom(original: PhotoFormValues): Map<PhotoField, String?> =
        PhotoField.entries.filter { valueOf(it) != original.valueOf(it) }.associateWith { valueOf(it) }
}

internal fun Photo.toFormValues(): PhotoFormValues =
    PhotoFormValues(title = title, comment = comment.orEmpty(), place = place.orEmpty())
