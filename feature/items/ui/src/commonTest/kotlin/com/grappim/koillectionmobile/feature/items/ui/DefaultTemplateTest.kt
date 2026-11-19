package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.ui.FakeChoiceLists.Companion.GENRES
import com.grappim.koillectionmobile.feature.items.ui.FakeCollectionsRepository.Companion.collectionWith
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_default_template_error
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The collection's default template, applied when the add form opens (M8.8). */
class DefaultTemplateTest {

    private val collections = FakeCollectionsRepository()
    private val tags = FakeTagsRepository()
    private val templates = FakeChoiceLists()
    private val savedStateHandle = SavedStateHandle()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        collections.answer = Result.success(collectionWith(defaultTemplateId = BOOK_ID))
        templates.fields = mapOf(BOOK_ID to Result.success(BOOK_FIELDS))
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun createSut(handle: SavedStateHandle = savedStateHandle): AddItemViewModel {
        tags.queue()
        return AddItemViewModel(
            "books",
            collections,
            tags,
            templates,
            FakeAddItemUseCase(),
            FakePhotoFiles(),
            ItemListChanges(),
            handle
        )
    }

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    private val AddItemViewModel.s: AddItemUiState get() = uiState.value

    @Test
    fun `the form opens with the default template's rows and slots`() {
        val sut = createSut()

        assertEquals(
            listOf(
                FieldRow(0, "Author", "", "text"),
                FieldRow(1, "Signed", "0", "checkbox"),
                FieldRow(2, "Genre", "[]", "choice-list", GENRES.id)
            ),
            sut.s.fields
        )
        assertEquals(listOf(ImageSlot(0, "Cover")), sut.s.imageSlots)
        assertEquals(listOf("Scan"), sut.s.skippedTemplateFields.map { it.name })
        assertEquals(listOf("books"), collections.getCalls)
        assertEquals(listOf(BOOK_ID), templates.fieldCalls)
    }

    @Test
    fun `a collection without a default template adds nothing and reads no fields`() {
        collections.answer = Result.success(collectionWith(defaultTemplateId = null))

        val sut = createSut()

        assertTrue(sut.s.fields.isEmpty())
        assertTrue(sut.s.imageSlots.isEmpty())
        assertTrue(templates.fieldCalls.isEmpty())
        createSut(restoredHandle())
        assertEquals(1, collections.getCalls.size, "a restore does not read the collection again")
    }

    @Test
    fun `the user can remove the rows and a saved-state restore does not apply them again`() {
        val first = createSut()
        first.s.onRemoveField(first.s.fields.first().id)
        first.s.onSlotRemove(first.s.imageSlots.single().id)

        val restored = createSut(restoredHandle())

        assertEquals(listOf("Signed", "Genre"), restored.s.fields.map { it.label })
        assertTrue(restored.s.imageSlots.isEmpty())
        assertEquals(1, collections.getCalls.size)
        assertEquals(1, templates.fieldCalls.size)
    }

    @Test
    fun `a failed collection read shows the error and the retry applies the template`() {
        collections.answer = Result.failure(KoillectionError.Network(IllegalStateException("offline")))
        val sut = createSut()
        assertEquals(NativeText.Resource(RString.add_item_default_template_error), sut.s.defaultTemplateError)
        assertTrue(sut.s.fields.isEmpty())

        collections.answer = Result.success(collectionWith(defaultTemplateId = BOOK_ID))
        sut.s.onRetryDefaultTemplate()

        assertTrue(sut.s.defaultTemplateError.isEmpty())
        assertEquals(3, sut.s.fields.size)
    }

    @Test
    fun `a failed field read shows the error and a restore tries again`() {
        templates.fields = mapOf(BOOK_ID to Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        val first = createSut()
        assertEquals(NativeText.Resource(RString.add_item_default_template_error), first.s.defaultTemplateError)

        templates.fields = mapOf(BOOK_ID to Result.success(BOOK_FIELDS))
        val restored = createSut(restoredHandle())

        assertEquals(3, restored.s.fields.size)
        assertTrue(restored.s.defaultTemplateError.isEmpty())
    }

    @Test
    fun `the default rows go after the rows the form has`() {
        templates.fields = mapOf(BOOK_ID to Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        val first = createSut()
        first.s.onAddField()
        first.s.onLabelChange(first.s.fields.single().id, "Author")

        templates.fields = mapOf(BOOK_ID to Result.success(BOOK_FIELDS))
        first.s.onRetryDefaultTemplate()

        assertEquals(listOf("Author", "Signed", "Genre"), first.s.fields.map { it.label })
    }

    private companion object {
        const val BOOK_ID = "book"

        val BOOK_FIELDS = listOf(
            TemplateField("f1", "Author", 1, "text", null),
            TemplateField("f2", "Cover", 2, "image", null),
            TemplateField("f3", "Signed", 3, "checkbox", null),
            TemplateField("f4", "Scan", 4, "file", null),
            TemplateField("f5", "Genre", 5, "choice-list", GENRES.id)
        )
    }
}
