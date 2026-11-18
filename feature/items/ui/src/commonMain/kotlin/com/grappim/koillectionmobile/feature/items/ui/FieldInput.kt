package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHECKBOX
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHOICE_LIST
import com.grappim.koillectionmobile.feature.items.domain.TYPE_NUMBER
import com.grappim.koillectionmobile.feature.items.domain.TYPE_PRICE
import com.grappim.koillectionmobile.feature.items.domain.TYPE_RATING
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_choice_lists_error
import kotlinx.serialization.json.Json

/*
 * The values of a typed field row, in the server's format for its type (M8.2, `Datum.php`). A row
 * keeps its value as the server string, so the save sends it as it is.
 */

/** The value of a new row of [type]: a `checkbox` is off, a `choice-list` has no pick, a text is empty. */
internal fun emptyValue(type: String): String = when (type) {
    TYPE_CHECKBOX -> CHECKBOX_OFF
    TYPE_CHOICE_LIST -> json.encodeToString(emptyList<String>())
    else -> ""
}

/** A type the user types the value of. A `checkbox` and a `choice-list` have their own input. */
internal fun isTyped(type: String): Boolean = type != TYPE_CHECKBOX && type != TYPE_CHOICE_LIST

/** The value after a type change from [from] to [to]: kept between two typed types, else the empty value. */
internal fun valueAfterTypeChange(value: String, from: String, to: String): String =
    if (isTyped(from) && isTyped(to)) value else emptyValue(to)

/** `Datum.php` takes `"1"` and `"0"` for a `checkbox`, nothing else (M8.2: `"true"` got 422). */
internal fun checkboxValue(isChecked: Boolean): String = if (isChecked) CHECKBOX_ON else CHECKBOX_OFF

internal fun isChecked(value: String): Boolean = value == CHECKBOX_ON

/** The picks of a `choice-list` value, a JSON array of strings. A value that is not one has no picks. */
internal fun pickedChoices(value: String): List<String> =
    runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

/**
 * [value] with [choice] picked, or unpicked if it was picked. The picks keep the order of
 * [choices]. A pick that is not in [choices] (the list changed on the server) stays, after them.
 */
internal fun withChoiceToggled(value: String, choice: String, choices: List<String>): String {
    val old = pickedChoices(value)
    val picked = if (choice in old) old - choice else old + choice
    return json.encodeToString(choices.filter { it in picked } + picked.filterNot { it in choices })
}

/**
 * A `number`, `price` or `rating` value the server rejects (`Datum.php`). An empty `number` or
 * `price` passes: Symfony's `Regex` does not check an empty string. A `rating` must be 1 to 10:
 * `Choice` checks an empty string too. The other types are checked by the server only.
 */
internal fun isValueInvalid(type: String, value: String): Boolean = when (type) {
    TYPE_NUMBER -> value.isNotEmpty() && !NUMBER.matches(value)
    TYPE_PRICE -> value.isNotEmpty() && !PRICE.matches(value)
    TYPE_RATING -> value !in RATINGS
    else -> false
}

private val RATINGS = (1..10).map { it.toString() }

private val NUMBER = Regex("""^-?(?:\d+|\d*\.\d+)$""")
private val PRICE = Regex("""^(?:\d+|\d*\.\d+)$""")

private const val CHECKBOX_ON = "1"
private const val CHECKBOX_OFF = "0"

private val json = Json

/** The server's choice lists, sorted by name (ignoring case, as the choice list screen), or the load's error. */
internal suspend fun TemplatesRepository.loadChoiceLists(): ChoiceListsUi = getChoiceLists().fold(
    onSuccess = { lists ->
        ChoiceListsUi(lists = lists.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }), isLoading = false)
    },
    onFailure = { ChoiceListsUi(isLoading = false, error = NativeText.Resource(RString.add_item_choice_lists_error)) }
)
