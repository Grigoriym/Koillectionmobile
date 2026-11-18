package com.grappim.koillectionmobile.feature.items.domain

/** The type names of `DatumTypeEnum` (Koillection 1.8.5) that the app handles by name. */
const val TYPE_TEXT = "text"
const val TYPE_TEXTAREA = "textarea"
const val TYPE_NUMBER = "number"
const val TYPE_PRICE = "price"
const val TYPE_RATING = "rating"
const val TYPE_CHECKBOX = "checkbox"
const val TYPE_CHOICE_LIST = "choice-list"
const val TYPE_IMAGE = "image"

/**
 * The text types: `DatumTypeEnum::TEXT_TYPES` without `file`, whose `value` is null. In the
 * enum's order. The item forms fill each one; the value goes to the server as a string.
 */
val TEXT_TYPES: List<String> = listOf(
    TYPE_TEXT,
    TYPE_TEXTAREA,
    "country",
    "date",
    TYPE_RATING,
    TYPE_NUMBER,
    TYPE_PRICE,
    "link",
    "list",
    TYPE_CHOICE_LIST,
    TYPE_CHECKBOX
)
