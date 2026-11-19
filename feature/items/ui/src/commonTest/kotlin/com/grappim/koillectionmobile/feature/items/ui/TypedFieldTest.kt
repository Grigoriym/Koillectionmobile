package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.NewTextField
import com.grappim.koillectionmobile.feature.items.ui.FakeChoiceLists.Companion.FORMATS
import com.grappim.koillectionmobile.feature.items.ui.FakeChoiceLists.Companion.GENRES
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_choice_lists_error
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The typed rows of the add form (M8.6): the input per type, the checks, and the value sent per type. */
class TypedFieldTest {

    private val tags = FakeTagsRepository()
    private val choiceLists = FakeChoiceLists()
    private val useCase = FakeAddItemUseCase()
    private val savedStateHandle = SavedStateHandle()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun createSut(handle: SavedStateHandle = savedStateHandle): AddItemViewModel {
        tags.queue()
        return AddItemViewModel(
            "books",
            FakeCollectionsRepository(),
            tags,
            choiceLists,
            useCase,
            FakePhotoFiles(),
            ItemListChanges(),
            handle
        )
    }

    private val AddItemViewModel.s: AddItemUiState get() = uiState.value

    /** Adds a row with [label] and [type] and returns its id. */
    private fun AddItemViewModel.addRow(label: String, type: String): Int {
        s.onAddField()
        val id = s.fields.last().id
        s.onLabelChange(id, label)
        s.onTypeChange(id, type)
        return id
    }

    private fun AddItemViewModel.row(id: Int): FieldRow = s.fields.single { it.id == id }

    @Test
    fun `a new row is a text row with an empty value`() {
        val sut = createSut()

        sut.s.onAddField()

        assertEquals(FieldRow(id = 0), sut.s.fields.single())
    }

    @Test
    fun `the choice lists load sorted by name ignoring case`() {
        val sut = createSut()

        assertEquals(1, choiceLists.calls)
        assertEquals(ChoiceListsUi(listOf(FORMATS, GENRES), isLoading = false), sut.s.choiceLists)
    }

    @Test
    fun `a failed choice list load shows the error and a retry loads again`() {
        choiceLists.answer = Result.failure(IllegalStateException("down"))
        val sut = createSut()
        assertEquals(NativeText.Resource(RString.add_item_choice_lists_error), sut.s.choiceLists.error)

        choiceLists.answer = Result.success(listOf(FORMATS))
        sut.s.onRetryChoiceLists()

        assertEquals(2, choiceLists.calls)
        assertEquals(ChoiceListsUi(listOf(FORMATS), isLoading = false), sut.s.choiceLists)
    }

    @Test
    fun `a checkbox row starts off and the switch sets 1 and 0`() {
        val sut = createSut()
        val id = sut.addRow("Signed", "checkbox")
        assertEquals("0", sut.row(id).value)

        sut.s.onCheckedChange(id, true)
        assertEquals("1", sut.row(id).value)

        sut.s.onCheckedChange(id, false)
        assertEquals("0", sut.row(id).value)
    }

    @Test
    fun `choice-list picks keep the order of the list and a new list drops them`() {
        val sut = createSut()
        val id = sut.addRow("Format", "choice-list")
        assertEquals(FieldRow(id, "Format", "[]", "choice-list", null), sut.row(id))

        sut.s.onChoiceListChange(id, FORMATS.id)
        sut.s.onChoiceToggle(id, "Ebook")
        sut.s.onChoiceToggle(id, "Hardcover")
        assertEquals("""["Hardcover","Ebook"]""", sut.row(id).value)

        sut.s.onChoiceToggle(id, "Ebook")
        assertEquals("""["Hardcover"]""", sut.row(id).value)

        sut.s.onChoiceListChange(id, GENRES.id)
        assertEquals(FieldRow(id, "Format", "[]", "choice-list", GENRES.id), sut.row(id))
    }

    @Test
    fun `a type change keeps a typed value and resets a switch or a pick`() {
        val sut = createSut()
        val id = sut.addRow("Pages", "text")
        sut.s.onValueChange(id, "412")

        sut.s.onTypeChange(id, "number")
        assertEquals("412", sut.row(id).value)

        sut.s.onTypeChange(id, "checkbox")
        assertEquals("0", sut.row(id).value)

        sut.s.onTypeChange(id, "choice-list")
        sut.s.onChoiceListChange(id, FORMATS.id)
        sut.s.onChoiceToggle(id, "Ebook")
        sut.s.onTypeChange(id, "text")
        assertEquals(FieldRow(id, "Pages", "", "text", null), sut.row(id))
    }

    @Test
    fun `save is off for a choice-list row without a list and for a value the server rejects`() {
        val sut = createSut()
        sut.s.onNameChange("Jörn Uhl")
        val list = sut.addRow("Format", "choice-list")
        assertFalse(sut.s.canSave, "no choice list")
        sut.s.onChoiceListChange(list, FORMATS.id)
        assertTrue(sut.s.canSave)

        val number = sut.addRow("Pages", "number")
        sut.s.onValueChange(number, "1,5")
        assertFalse(sut.s.canSave, "a comma in a number")
        sut.s.onValueChange(number, "-1.5")
        assertTrue(sut.s.canSave)

        val price = sut.addRow("Price", "price")
        sut.s.onValueChange(price, "-3")
        assertFalse(sut.s.canSave, "a negative price")
        sut.s.onValueChange(price, "")
        assertTrue(sut.s.canSave, "an empty price")

        val rating = sut.addRow("Rating", "rating")
        assertFalse(sut.s.canSave, "an empty rating")
        sut.s.onValueChange(rating, "11")
        assertFalse(sut.s.canSave, "a rating over 10")
        sut.s.onValueChange(rating, "10")
        assertTrue(sut.s.canSave)
    }

    @Test
    fun `save sends each row with its type its value and its choice list`() {
        val sut = createSut()
        sut.s.onNameChange("Jörn Uhl")
        val signed = sut.addRow("Signed", "checkbox")
        sut.s.onCheckedChange(signed, true)
        sut.addRow("Read", "checkbox")
        val format = sut.addRow("Format", "choice-list")
        sut.s.onChoiceListChange(format, FORMATS.id)
        sut.s.onChoiceToggle(format, "Ebook")
        sut.s.onChoiceToggle(format, "Hardcover")
        val pages = sut.addRow("Pages", "number")
        sut.s.onValueChange(pages, "412")
        val date = sut.addRow("Published", "date")
        sut.s.onValueChange(date, "1901-10-01")
        val notes = sut.addRow("Notes", "text")
        sut.s.onValueChange(notes, "First edition")

        sut.s.onSave()

        assertEquals(
            listOf(
                NewTextField("Signed", "1", "checkbox"),
                NewTextField("Read", "0", "checkbox"),
                NewTextField("Format", """["Hardcover","Ebook"]""", "choice-list", FORMATS.id),
                NewTextField("Pages", "412", "number"),
                NewTextField("Published", "1901-10-01", "date"),
                NewTextField("Notes", "First edition", "text")
            ),
            useCase.calls.single().first.textFields
        )
    }

    @Test
    fun `typed rows survive process death`() {
        val sut = createSut()
        val format = sut.addRow("Format", "choice-list")
        sut.s.onChoiceListChange(format, FORMATS.id)
        sut.s.onChoiceToggle(format, "Paperback")
        val restored = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

        val again = createSut(restored)

        assertEquals(FieldRow(format, "Format", """["Paperback"]""", "choice-list", FORMATS.id), again.row(format))
        assertNull(again.s.save)
    }

    @Test
    fun `a pick that is not in the list stays after the list's choices`() {
        assertEquals(
            """["Hardcover","Signed copy"]""",
            withChoiceToggled("""["Signed copy"]""", "Hardcover", FORMATS.choices)
        )
        assertEquals(emptyList(), pickedChoices("not json"))
    }
}
