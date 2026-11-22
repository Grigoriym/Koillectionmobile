package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WishFormValuesTest {

    @Test
    fun `an empty currency is valid`() {
        assertTrue(WishFormValues(name = "Tent").isCurrencyValid)
        assertTrue(WishFormValues(name = "Tent", currency = "   ").isCurrencyValid)
    }

    @Test
    fun `three letters are a valid currency once upper-cased and trimmed`() {
        val values = WishFormValues(name = "Tent").with(WishField.CURRENCY, " eur ")

        assertEquals(" EUR ", values.currency)
        assertTrue(values.isCurrencyValid)
        assertEquals("EUR", values.valueOf(WishField.CURRENCY))
    }

    @Test
    fun `a currency that is not three letters blocks the save`() {
        listOf("EURO", "EU", "E1R", "€UR").forEach { code ->
            val values = WishFormValues(name = "Tent", currency = code)
            assertFalse(values.isCurrencyValid, code)
            assertFalse(values.canSave, code)
        }
    }

    @Test
    fun `a blank name blocks the save`() {
        assertFalse(WishFormValues(name = "  ").canSave)
        assertTrue(WishFormValues(name = "Tent").canSave)
    }

    @Test
    fun `an empty field has no value and a filled one is trimmed`() {
        val values = WishFormValues(name = " Tent ", url = "", comment = " light ")

        assertEquals("Tent", values.valueOf(WishField.NAME))
        assertNull(values.valueOf(WishField.URL))
        assertEquals("light", values.valueOf(WishField.COMMENT))
    }

    @Test
    fun `each changed field alone is the only change`() {
        WishField.entries.forEach { field ->
            val edited = FULL.with(field, "XYZ")

            assertEquals(mapOf(field to "XYZ"), edited.changesFrom(FULL), field.name)
        }
    }

    @Test
    fun `a cleared field is a change to null`() {
        val edited = FULL.with(WishField.URL, "  ")

        assertEquals(mapOf(WishField.URL to null), edited.changesFrom(FULL))
    }

    @Test
    fun `only whitespace around a value is no change`() {
        val edited = FULL.with(WishField.PRICE, " 12.50 ").with(WishField.CURRENCY, "eur")

        assertTrue(edited.changesFrom(FULL).isEmpty())
    }

    private companion object {
        val FULL = WishFormValues(
            name = "Tent",
            url = "https://shop.test/tent",
            price = "12.50",
            currency = "EUR",
            comment = "light"
        )
    }
}
