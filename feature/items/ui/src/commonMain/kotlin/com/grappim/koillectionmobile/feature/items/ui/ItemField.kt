package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.TEXT_TYPES
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHECKBOX
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHOICE_LIST

/** One field of the item, in the form the screen shows it. */
sealed interface ItemField {
    val id: String
    val label: String

    data class Text(override val id: String, override val label: String, val value: String) : ItemField

    /** A `checkbox`: the screen shows yes or no. */
    data class Checkbox(override val id: String, override val label: String, val isChecked: Boolean) : ItemField

    data class Picture(override val id: String, override val label: String, val image: Image) : ItemField

    /** A type the screen does not show, or an image field without an uploaded image. */
    data class NotShown(override val id: String, override val label: String, val type: String) : ItemField
}

/** The types that carry an uploaded image (`DatumTypeEnum::IMAGE_TYPES` without `video`). */
private val IMAGE_TYPES = setOf("image", "sign")

/**
 * A `checkbox` shows yes or no, and a `choice-list` its picks joined by commas. Another text type's
 * value shows as the server sends it: a `date` or a `list` is not formatted.
 */
internal fun Datum.toField(): ItemField {
    val label = label.orEmpty()
    val image = image
    return when {
        type == TYPE_CHECKBOX -> ItemField.Checkbox(id = id, label = label, isChecked = isChecked(value.orEmpty()))

        type == TYPE_CHOICE_LIST ->
            ItemField.Text(id = id, label = label, value = pickedChoices(value.orEmpty()).joinToString(", "))

        type in TEXT_TYPES -> ItemField.Text(id = id, label = label, value = value.orEmpty())

        type in IMAGE_TYPES && image != null -> ItemField.Picture(id = id, label = label, image = image)

        else -> ItemField.NotShown(id = id, label = label, type = type)
    }
}
