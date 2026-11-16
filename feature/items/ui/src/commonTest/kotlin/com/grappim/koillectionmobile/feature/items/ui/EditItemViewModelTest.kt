package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.item_error_network
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditItemViewModelTest {

    private val items = FakeItemsRepository()
    private val tags = FakeTagsRepository()
    private val photoFiles = FakePhotoFiles()
    private val savedStateHandle = SavedStateHandle()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        tags.itemTags = Result.success(listOf(NOVEL))
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    /** Creates the ViewModel and answers the tag list at once. */
    private fun createSut(handle: SavedStateHandle = savedStateHandle): EditItemViewModel {
        tags.queue()
        val sut = EditItemViewModel(DUNE.id, items, tags, photoFiles, handle)
        tags.tags.removeFirst().complete(Result.success(listOf(NOVEL, GERMAN)))
        tags.categories.removeFirst().complete(Result.success(emptyList<TagCategory>()))
        return sut
    }

    private val draft get(): EditItemDraft = checkNotNull(createdSut.uiState.value.draft)

    private lateinit var createdSut: EditItemViewModel

    private fun loaded(): EditItemViewModel = createSut().also { createdSut = it }

    @Test
    fun `the form is filled from the server`() {
        val sut = loaded()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(DUNE.id), items.itemCalls)
        assertEquals(listOf(DUNE.id), items.dataCalls)
        assertEquals(listOf(DUNE.id), tags.itemTagCalls)
        assertEquals("Dune", draft.name)
        assertEquals("https://k.test/uploads/dune-large.jpg", draft.mainImage.previewUrl)
        assertNull(draft.mainImage.uri)
        assertEquals(
            listOf(
                EditRow.Text(id = 2, datumId = "author", label = "Author", value = "Frank Herbert"),
                EditRow.ReadOnly(id = 3, label = "Published", datumType = "date", value = "1965-08-01")
            ),
            draft.fields
        )
        assertEquals(
            listOf(
                EditPhoto(
                    id = 1,
                    datumId = "title-page",
                    previewUrl = "https://k.test/uploads/page.jpg",
                    label = "Title page"
                )
            ),
            draft.photos
        )
        assertEquals(setOf(NOVEL.id), draft.selectedTagIds)
        assertEquals(state.draft, state.original)
        assertFalse(state.hasChanges)
        assertEquals(2, state.tagGroups.single().tags.size)
    }

    @Test
    fun `an item without a main image has an empty main image row`() {
        items.item = Result.success(DUNE.copy(image = null))

        loaded()

        assertNull(draft.mainImage.previewUrl)
        assertNull(draft.mainImage.uri)
    }

    @Test
    fun `a failed load shows the error and a retry fills the form`() {
        items.item = Result.failure(KoillectionError.Network(Exception("offline")))
        val sut = loaded()

        assertNull(sut.uiState.value.draft)
        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.item_error_network), sut.uiState.value.error)

        items.item = Result.success(DUNE)
        sut.uiState.value.onRetry()

        assertEquals("Dune", draft.name)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(2, items.itemCalls.size)
    }

    @Test
    fun `text edits change the draft and not the original`() {
        val sut = loaded()
        val author = draft.fields.first().id

        sut.uiState.value.onNameChange("Dune Messiah")
        sut.uiState.value.onLabelChange(author, "Writer")
        sut.uiState.value.onValueChange(author, "F. Herbert")
        sut.uiState.value.onTagToggle(NOVEL.id)
        sut.uiState.value.onTagToggle(GERMAN.id)

        assertEquals("Dune Messiah", draft.name)
        assertEquals(EditRow.Text(author, "author", "Writer", "F. Herbert"), draft.fields.first())
        assertEquals(setOf(GERMAN.id), draft.selectedTagIds)
        assertEquals("Dune", sut.uiState.value.original?.name)
        assertTrue(sut.uiState.value.hasChanges)
    }

    @Test
    fun `a text field can be removed and a new one added after the rest`() {
        val sut = loaded()
        val author = draft.fields.first().id

        sut.uiState.value.onRemoveField(author)
        sut.uiState.value.onAddField()

        assertEquals(2, draft.fields.size)
        assertTrue(draft.fields[0] is EditRow.ReadOnly)
        val added = draft.fields[1] as EditRow.Text
        assertNull(added.datumId)
        assertEquals(4, added.id)
    }

    @Test
    fun `a read-only field is not removed or changed`() {
        val sut = loaded()
        val published = draft.fields[1].id

        sut.uiState.value.onRemoveField(published)
        sut.uiState.value.onLabelChange(published, "Year")

        assertEquals(sut.uiState.value.original?.fields, draft.fields)
    }

    @Test
    fun `a replace takes the first photo and deletes the rest`() {
        val sut = loaded()
        val main = draft.mainImage.id

        sut.uiState.value.onPhotoReplace(main, listOf("content://a", "content://b"))

        assertEquals("content://a", draft.mainImage.uri)
        assertEquals("https://k.test/uploads/dune-large.jpg", draft.mainImage.previewUrl)
        assertEquals(listOf("content://b"), photoFiles.deleted)
    }

    @Test
    fun `a second replace deletes the first replacement and resets the rotation`() {
        val sut = loaded()
        val page = draft.photos.single().id
        sut.uiState.value.onPhotoReplace(page, listOf("content://a"))
        sut.uiState.value.onPhotoRotate(page)

        sut.uiState.value.onPhotoReplace(page, listOf("content://c"))

        assertEquals("content://c", draft.photos.single().uri)
        assertEquals(Rotation.NONE, draft.photos.single().rotation)
        assertEquals(listOf("content://a"), photoFiles.deleted)
    }

    @Test
    fun `only a photo from the device rotates`() {
        val sut = loaded()
        val main = draft.mainImage.id

        sut.uiState.value.onPhotoRotate(main)
        assertEquals(Rotation.NONE, draft.mainImage.rotation)

        sut.uiState.value.onPhotoReplace(main, listOf("content://a"))
        sut.uiState.value.onPhotoRotate(main)
        assertEquals(Rotation.entries[1], draft.mainImage.rotation)
    }

    @Test
    fun `an image field can be removed but the main image cannot`() {
        val sut = loaded()
        val page = draft.photos.single().id
        sut.uiState.value.onPhotoReplace(page, listOf("content://a"))

        sut.uiState.value.onPhotoRemove(page)
        sut.uiState.value.onPhotoRemove(draft.mainImage.id)

        assertTrue(draft.photos.isEmpty())
        assertEquals(sut.uiState.value.original?.mainImage, draft.mainImage)
        assertEquals(listOf("content://a"), photoFiles.deleted)
    }

    @Test
    fun `new photos become image fields after the existing ones`() {
        val sut = loaded()

        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        sut.uiState.value.onPhotoLabelChange(draft.photos[1].id, "Back cover")

        assertEquals(listOf("title-page", null, null), draft.photos.map { it.datumId })
        assertEquals(listOf(null, "content://a", "content://b"), draft.photos.map { it.uri })
        assertEquals("Back cover", draft.photos[1].label)
    }

    @Test
    fun `a label used by a text field and an image field is a duplicate`() {
        val sut = loaded()

        sut.uiState.value.onLabelChange(draft.fields.first().id, "Title page")
        assertEquals(setOf("Title page"), sut.uiState.value.duplicateLabels)

        sut.uiState.value.onLabelChange(draft.fields.first().id, "Published")
        assertEquals(setOf("Published"), sut.uiState.value.duplicateLabels)
    }

    @Test
    fun `after a process death the edits come back and the item is not read again`() {
        val sut = loaded()
        sut.uiState.value.onNameChange("Dune Messiah")
        sut.uiState.value.onPhotoReplace(draft.mainImage.id, listOf("content://a"))

        val restored =
            createSut(SavedStateHandle(mapOf("edit_item_form" to savedStateHandle.get<String>("edit_item_form"))))

        val state = restored.uiState.value
        assertEquals(1, items.itemCalls.size)
        assertFalse(state.isLoading)
        assertEquals("Dune Messiah", state.draft?.name)
        assertEquals("content://a", state.draft?.mainImage?.uri)
        assertEquals("Dune", state.original?.name)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `rows added after a restore get ids the restored rows do not use`() {
        val sut = loaded()
        sut.uiState.value.onAddField()

        val restored =
            createSut(SavedStateHandle(mapOf("edit_item_form" to savedStateHandle.get<String>("edit_item_form"))))
        restored.uiState.value.onAddField()

        val ids = checkNotNull(restored.uiState.value.draft).fields.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `leaving the form deletes its photo files`() {
        val sut = loaded()
        sut.uiState.value.onPhotoReplace(draft.mainImage.id, listOf("content://a"))
        sut.uiState.value.onPhotosAdd(listOf("content://b"))
        val store = ViewModelStore()
        store.put("edit-item", sut)

        store.clear()

        assertEquals(listOf("content://a", "content://b"), photoFiles.deleted)
    }

    private class FakeItemsRepository : ItemsRepository {
        var item: Result<Item> = Result.success(DUNE)
        var data: Result<List<Datum>> = Result.success(listOf(AUTHOR, TITLE_PAGE, PUBLISHED))
        val itemCalls = mutableListOf<String>()
        val dataCalls = mutableListOf<String>()

        override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

        override suspend fun getItem(id: String): Result<Item> {
            itemCalls += id
            return item
        }

        override suspend fun getData(itemId: String): Result<List<Datum>> {
            dataCalls += itemId
            return data
        }

        override suspend fun createItem(collectionId: String, name: String): Result<Item> = error("not used")

        override suspend fun createTextDatum(
            itemId: String,
            label: String,
            value: String,
            position: Int
        ): Result<Datum> = error("not used")

        override suspend fun createImageDatum(itemId: String, label: String, position: Int): Result<Datum> =
            error("not used")

        override suspend fun uploadItemImage(
            itemId: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Item> = error("not used")

        override suspend fun uploadDatumImage(
            datumId: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Datum> = error("not used")

        override suspend fun renameItem(id: String, name: String): Result<Item> = error("not used")

        override suspend fun updateDatum(id: String, label: String?, value: String?): Result<Datum> = error("not used")

        override suspend fun deleteDatum(id: String): Result<Unit> = error("not used")

        override suspend fun deleteItem(id: String): Result<Unit> = error("not used")
    }

    private companion object {
        val DUNE = Item(
            id = "dune",
            name = "Dune",
            quantity = 1,
            collectionId = "books",
            image = Image(
                url = "https://k.test/uploads/dune.jpg",
                smallThumbnailUrl = null,
                largeThumbnailUrl = "https://k.test/uploads/dune-large.jpg"
            )
        )
        val AUTHOR = Datum("author", "text", "Author", "Frank Herbert", 1, null)
        val TITLE_PAGE = Datum(
            id = "title-page",
            type = "image",
            label = "Title page",
            value = null,
            position = 2,
            image = Image("https://k.test/uploads/page.jpg", null, null)
        )
        val PUBLISHED = Datum("published", "date", "Published", "1965-08-01", 3, null)
        val NOVEL = Tag(id = "novel", label = "Novel", description = null, categoryId = null)
        val GERMAN = Tag(id = "german", label = "German", description = null, categoryId = null)
    }
}
