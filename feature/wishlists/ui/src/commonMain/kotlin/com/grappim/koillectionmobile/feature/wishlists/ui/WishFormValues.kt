package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import kotlinx.serialization.Serializable

/**
 * The text fields of the wish form, as the user typed them. An empty field is no value: the server
 * gets null for it ([valueOf]).
 */
@Serializable
data class WishFormValues(
    val name: String = "",
    val url: String = "",
    val price: String = "",
    val currency: String = "",
    val comment: String = ""
) {
    /**
     * Empty, or 3 letters (plan §5 decision 12). The server checks that the code exists
     * (`Assert\Currency` in `Wish.php`, a 422).
     */
    val isCurrencyValid: Boolean
        get() {
            val code = valueOf(WishField.CURRENCY) ?: return true
            return code.length == CURRENCY_LENGTH && code.all { it in 'A'..'Z' }
        }

    /** The server needs a name (`NotBlank` in `Wish.php`). */
    val canSave: Boolean
        get() = name.isNotBlank() && isCurrencyValid

    fun text(field: WishField): String = when (field) {
        WishField.NAME -> name
        WishField.URL -> url
        WishField.PRICE -> price
        WishField.CURRENCY -> currency
        WishField.COMMENT -> comment
    }

    /** A copy with [text] in [field]. The currency is upper-cased as the user types. */
    fun with(field: WishField, text: String): WishFormValues = when (field) {
        WishField.NAME -> copy(name = text)
        WishField.URL -> copy(url = text)
        WishField.PRICE -> copy(price = text)
        WishField.CURRENCY -> copy(currency = text.uppercase())
        WishField.COMMENT -> copy(comment = text)
    }

    /** The value of [field] as the server gets it: trimmed and upper-cased for the currency, or null when empty. */
    fun valueOf(field: WishField): String? {
        val value = text(field).trim().ifEmpty { return null }
        return if (field == WishField.CURRENCY) value.uppercase() else value
    }

    /**
     * The fields whose value differs from [original], with the new value. A null value clears the
     * field: the server takes an explicit `null` (M9.2, plan §2).
     */
    fun changesFrom(original: WishFormValues): Map<WishField, String?> =
        WishField.entries.filter { valueOf(it) != original.valueOf(it) }.associateWith { valueOf(it) }
}

internal fun Wish.toFormValues(): WishFormValues = WishFormValues(
    name = name,
    url = url.orEmpty(),
    price = price.orEmpty(),
    currency = currency.orEmpty(),
    comment = comment.orEmpty()
)

private const val CURRENCY_LENGTH = 3
