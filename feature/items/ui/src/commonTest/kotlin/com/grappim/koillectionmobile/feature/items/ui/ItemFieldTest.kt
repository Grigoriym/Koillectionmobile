package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.feature.items.domain.Datum
import kotlin.test.Test
import kotlin.test.assertEquals

/** How the item screen shows a typed value (M14.15): a `checkbox` as yes or no, a `choice-list` as its picks. */
class ItemFieldTest {

    private fun datum(type: String, value: String?) =
        Datum(id = "d1", type = type, label = "Label", value = value, position = 1, image = null)

    @Test
    fun `checkbox 1 is checked`() {
        assertEquals(ItemField.Checkbox("d1", "Label", isChecked = true), datum("checkbox", "1").toField())
    }

    @Test
    fun `checkbox 0 is not checked`() {
        assertEquals(ItemField.Checkbox("d1", "Label", isChecked = false), datum("checkbox", "0").toField())
    }

    @Test
    fun `filled choice list shows the picks joined by commas`() {
        assertEquals(
            ItemField.Text("d1", "Label", "Novel, Poem"),
            datum("choice-list", """["Novel","Poem"]""").toField()
        )
    }

    @Test
    fun `empty choice list shows nothing`() {
        assertEquals(ItemField.Text("d1", "Label", ""), datum("choice-list", "[]").toField())
    }

    @Test
    fun `text value shows as the server sends it`() {
        assertEquals(ItemField.Text("d1", "Label", "2024-01-31"), datum("date", "2024-01-31").toField())
    }
}
