package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image

/** One field of the item, in the form the screen shows it. */
sealed interface ItemField {
    val id: String
    val label: String

    data class Text(override val id: String, override val label: String, val value: String) : ItemField

    data class Picture(override val id: String, override val label: String, val image: Image) : ItemField

    /** A type the screen does not show, or an image field without an uploaded image. */
    data class NotShown(override val id: String, override val label: String, val type: String) : ItemField
}

/**
 * The server's text types (`DatumTypeEnum::TEXT_TYPES` in Koillection 1.8.5) without `file`, whose
 * `value` is null. The value shows as the server sends it: a `date` or a `list` is not formatted.
 */
private val TEXT_TYPES = setOf(
    "text",
    "textarea",
    "country",
    "date",
    "rating",
    "number",
    "price",
    "link",
    "list",
    "choice-list",
    "checkbox"
)

/** The types that carry an uploaded image (`DatumTypeEnum::IMAGE_TYPES` without `video`). */
private val IMAGE_TYPES = setOf("image", "sign")

internal fun Datum.toField(): ItemField {
    val label = label.orEmpty()
    val image = image
    return when {
        type in TEXT_TYPES -> ItemField.Text(id = id, label = label, value = value.orEmpty())
        type in IMAGE_TYPES && image != null -> ItemField.Picture(id = id, label = label, image = image)
        else -> ItemField.NotShown(id = id, label = label, type = type)
    }
}
