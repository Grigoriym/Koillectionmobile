package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.NewTextField
import com.grappim.koillectionmobile.feature.items.ui.FakeChoiceLists.Companion.GENRES
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_templates_error
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A template applied in the add form (M8.7): the rows and image slots it adds, and what it skips. */
class TemplateApplyTest {

    private val collections = FakeCollectionsRepository()
    private val tags = FakeTagsRepository()
    private val templates = FakeChoiceLists()
    private val useCase = FakeAddItemUseCase()
    private val photoFiles = FakePhotoFiles()
    private val savedStateHandle = SavedStateHandle()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        templates.templates = Result.success(listOf(BOOK, ALBUM))
        templates.fields = mapOf(BOOK.id to Result.success(BOOK_FIELDS), ALBUM.id to Result.success(ALBUM_FIELDS))
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun createSut(handle: SavedStateHandle = savedStateHandle): AddItemViewModel {
        tags.queue()
        return AddItemViewModel("books", collections, tags, templates, useCase, photoFiles, ItemListChanges(), handle)
    }

    private val AddItemViewModel.s: AddItemUiState get() = uiState.value

    private fun AddItemViewModel.apply(template: Template) {
        s.onOpenTemplates()
        s.onTemplatePick(template.id)
    }

    @Test
    fun `the picker lists the templates sorted by name ignoring case`() {
        val sut = createSut()

        sut.s.onOpenTemplates()

        assertEquals(TemplatePickerUi(templates = listOf(ALBUM, BOOK), isLoading = false), sut.s.templatePicker)
        assertEquals(1, templates.templateCalls)
    }

    @Test
    fun `apply adds the fields as empty typed rows after the form's rows and closes the picker`() {
        val sut = createSut()
        sut.s.onAddField()
        val own = sut.s.fields.single()
        sut.s.onLabelChange(own.id, "Notes")

        sut.apply(BOOK)

        assertEquals(
            listOf(
                FieldRow(own.id, "Notes"),
                FieldRow(own.id + 1, "Author", "", "text"),
                FieldRow(own.id + 2, "Signed", "0", "checkbox"),
                FieldRow(own.id + 3, "Genre", "[]", "choice-list", GENRES.id),
                FieldRow(own.id + 4, "Pages", "", "number")
            ),
            sut.s.fields
        )
        assertEquals(listOf(ImageSlot(0, "Cover")), sut.s.imageSlots)
        assertNull(sut.s.templatePicker)
        assertEquals(listOf(BOOK.id), templates.fieldCalls)
    }

    @Test
    fun `the skipped types are named and the note can be dismissed`() {
        val sut = createSut()

        sut.apply(BOOK)

        assertEquals(
            listOf("Signature", "Scan", "Trailer", "Details", "Gap"),
            sut.s.skippedTemplateFields.map {
                it.name
            }
        )
        sut.s.onSkippedDismiss()
        assertTrue(sut.s.skippedTemplateFields.isEmpty())
    }

    @Test
    fun `a second apply of the same template adds nothing`() {
        val sut = createSut()
        sut.apply(BOOK)
        val fields = sut.s.fields
        val slots = sut.s.imageSlots

        sut.apply(BOOK)

        assertEquals(fields, sut.s.fields)
        assertEquals(slots, sut.s.imageSlots)
    }

    @Test
    fun `a label the form has is not added twice`() {
        val sut = createSut()
        sut.s.onAddField()
        sut.s.onLabelChange(sut.s.fields.single().id, "Author")
        sut.s.onPhotosAdd(listOf("content://main", "content://spine"))
        sut.s.onPhotoLabelChange(sut.s.photos[1].id, "Cover")
        sut.apply(ALBUM)

        sut.apply(BOOK)

        assertEquals(listOf("Author", "Artist", "Signed", "Genre", "Pages"), sut.s.fields.map { it.label })
        assertEquals(listOf("Booklet"), sut.s.imageSlots.map { it.label })
    }

    @Test
    fun `a name used twice in one template is added once`() {
        val sut = createSut()
        templates.fields =
            mapOf(BOOK.id to Result.success(listOf(field("Author", "text"), field("Author", "textarea"))))

        sut.apply(BOOK)

        assertEquals(listOf(FieldRow(0, "Author", "", "text")), sut.s.fields)
    }

    @Test
    fun `a failed load shows the error and a retry loads the picked template again`() {
        val sut = createSut()
        templates.fields = mapOf(BOOK.id to Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        sut.apply(BOOK)
        assertEquals(NativeText.Resource(RString.add_item_templates_error), sut.s.templatePicker?.error)
        assertTrue(sut.s.fields.isEmpty())

        templates.fields = mapOf(BOOK.id to Result.success(BOOK_FIELDS))
        sut.s.onRetryTemplates()

        assertEquals(listOf(BOOK.id, BOOK.id), templates.fieldCalls)
        assertEquals(4, sut.s.fields.size)
        assertNull(sut.s.templatePicker)
    }

    @Test
    fun `a failed list load shows the error and a retry loads the list again`() {
        val sut = createSut()
        templates.templates = Result.failure(KoillectionError.Network(IllegalStateException("offline")))
        sut.s.onOpenTemplates()
        assertEquals(NativeText.Resource(RString.add_item_templates_error), sut.s.templatePicker?.error)

        templates.templates = Result.success(listOf(BOOK))
        sut.s.onRetryTemplates()

        assertEquals(TemplatePickerUi(templates = listOf(BOOK), isLoading = false), sut.s.templatePicker)
    }

    @Test
    fun `a filled slot is sent as an image field and an empty slot is dropped`() {
        val sut = createSut()
        templates.fields = mapOf(BOOK.id to Result.success(listOf(field("Cover", "image"), field("Back", "image"))))
        sut.s.onNameChange("Jörn Uhl")
        sut.s.onPhotosAdd(listOf("content://main"))
        sut.apply(BOOK)
        val cover = sut.s.imageSlots.first()
        sut.s.onSlotPhotos(cover.id, listOf("content://cover", "content://extra"))
        sut.s.onSlotRotate(cover.id)
        assertTrue(sut.s.canSave, "an empty slot does not block the save")

        sut.s.onSave()

        val item = useCase.calls.single().first
        assertEquals("content://main@${Rotation.NONE}", item.mainImage?.bytes?.decodeToString())
        assertEquals(listOf("Cover"), item.imageFields.map { it.label })
        assertEquals("content://cover@${Rotation.CW_90}", item.imageFields.single().image.bytes.decodeToString())
        assertTrue("content://extra" in photoFiles.deleted, "a second pick for one slot is deleted")
    }

    @Test
    fun `a filled slot with a label another field has blocks the save`() {
        val sut = createSut()
        templates.fields = mapOf(BOOK.id to Result.success(listOf(field("Cover", "image"))))
        sut.s.onNameChange("Jörn Uhl")
        sut.apply(BOOK)
        val cover = sut.s.imageSlots.single()
        sut.s.onAddField()
        sut.s.onLabelChange(sut.s.fields.single().id, "Cover")
        assertTrue(sut.s.canSave, "an empty slot sends nothing so its label clashes with nothing")

        sut.s.onSlotPhotos(cover.id, listOf("content://cover"))

        assertEquals(setOf("Cover"), sut.s.duplicateLabels)
        assertFalse(sut.s.canSave)
    }

    @Test
    fun `removing a slot deletes its photo`() {
        val sut = createSut()
        sut.apply(BOOK)
        val cover = sut.s.imageSlots.single()
        sut.s.onSlotPhotos(cover.id, listOf("content://cover"))

        sut.s.onSlotRemove(cover.id)

        assertTrue(sut.s.imageSlots.isEmpty())
        assertEquals(listOf("content://cover"), photoFiles.deleted)
    }

    @Test
    fun `applied rows and slots survive process death`() {
        val sut = createSut()
        sut.apply(BOOK)
        sut.s.onSlotPhotos(sut.s.imageSlots.single().id, listOf("content://cover"))
        val restored = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

        val again = createSut(restored)

        assertEquals(sut.s.fields, again.s.fields)
        assertEquals(listOf(ImageSlot(0, "Cover", "content://cover")), again.s.imageSlots)
        again.apply(ALBUM)
        assertEquals(1, again.s.imageSlots.last().id, "a new slot gets a new id")
    }

    @Test
    fun `the applied rows are sent with their types`() {
        val sut = createSut()
        sut.s.onNameChange("Jörn Uhl")
        sut.apply(BOOK)
        val genre = sut.s.fields.single { it.label == "Genre" }
        sut.s.onChoiceToggle(genre.id, "Novel")

        sut.s.onSave()

        assertEquals(
            listOf(
                NewTextField("Author", "", "text"),
                NewTextField("Signed", "0", "checkbox"),
                NewTextField("Genre", """["Novel"]""", "choice-list", GENRES.id),
                NewTextField("Pages", "", "number")
            ),
            useCase.calls.single().first.textFields
        )
        assertTrue(useCase.calls.single().first.imageFields.isEmpty())
    }

    private companion object {
        val BOOK = Template("book", "book")
        val ALBUM = Template("album", "Album")

        var nextFieldId = 0

        fun field(name: String, type: String, choiceListId: String? = null) = TemplateField(
            id = "f${nextFieldId++}",
            name = name,
            position = nextFieldId,
            type = type,
            choiceListId = choiceListId
        )

        val BOOK_FIELDS = listOf(
            field("Author", "text"),
            field("Cover", "image"),
            field("Signature", "sign"),
            field("Signed", "checkbox"),
            field("Scan", "file"),
            field("Genre", "choice-list", GENRES.id),
            field("Trailer", "video"),
            field("Details", "section"),
            field("Gap", "blank-line"),
            field("Pages", "number")
        )

        val ALBUM_FIELDS = listOf(field("Artist", "text"), field("Cover", "image"), field("Booklet", "image"))
    }
}
