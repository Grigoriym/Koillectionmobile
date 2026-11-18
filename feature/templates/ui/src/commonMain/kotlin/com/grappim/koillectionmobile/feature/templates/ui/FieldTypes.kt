package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.field_type_blank_line
import com.grappim.koillectionmobile.strings.generated.resources.field_type_checkbox
import com.grappim.koillectionmobile.strings.generated.resources.field_type_choice_list
import com.grappim.koillectionmobile.strings.generated.resources.field_type_country
import com.grappim.koillectionmobile.strings.generated.resources.field_type_date
import com.grappim.koillectionmobile.strings.generated.resources.field_type_file
import com.grappim.koillectionmobile.strings.generated.resources.field_type_image
import com.grappim.koillectionmobile.strings.generated.resources.field_type_link
import com.grappim.koillectionmobile.strings.generated.resources.field_type_list
import com.grappim.koillectionmobile.strings.generated.resources.field_type_number
import com.grappim.koillectionmobile.strings.generated.resources.field_type_price
import com.grappim.koillectionmobile.strings.generated.resources.field_type_rating
import com.grappim.koillectionmobile.strings.generated.resources.field_type_section
import com.grappim.koillectionmobile.strings.generated.resources.field_type_sign
import com.grappim.koillectionmobile.strings.generated.resources.field_type_text
import com.grappim.koillectionmobile.strings.generated.resources.field_type_textarea
import com.grappim.koillectionmobile.strings.generated.resources.field_type_video
import org.jetbrains.compose.resources.StringResource

/**
 * The field types of the type picker: all of `DatumTypeEnum::TYPES` (Koillection 1.8.5), in its
 * order, with a readable label each (user, 2026-11-18, M8.4).
 */
internal val FIELD_TYPES: List<Pair<String, StringResource>> = listOf(
    TYPE_TEXT to RString.field_type_text,
    "textarea" to RString.field_type_textarea,
    "country" to RString.field_type_country,
    "date" to RString.field_type_date,
    "rating" to RString.field_type_rating,
    "number" to RString.field_type_number,
    "price" to RString.field_type_price,
    "link" to RString.field_type_link,
    "list" to RString.field_type_list,
    TYPE_CHOICE_LIST to RString.field_type_choice_list,
    "checkbox" to RString.field_type_checkbox,
    "image" to RString.field_type_image,
    "file" to RString.field_type_file,
    "sign" to RString.field_type_sign,
    "video" to RString.field_type_video,
    "blank-line" to RString.field_type_blank_line,
    "section" to RString.field_type_section
)

/** The label of [type], or null for a type this app does not know (the server's value is shown then). */
internal fun fieldTypeLabel(type: String): StringResource? = FIELD_TYPES.firstOrNull { it.first == type }?.second

internal const val TYPE_TEXT = "text"
internal const val TYPE_CHOICE_LIST = "choice-list"
