package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.item_error_http
import com.grappim.koillectionmobile.strings.generated.resources.item_error_network
import com.grappim.koillectionmobile.strings.generated.resources.item_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemViewModelTest {

    private val items = FakeItemsRepository()
    private val tags = FakeTagsRepository()
    private val itemListChanges = ItemListChanges()
    private val itemChanges = ItemChanges()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun createSut() = ItemViewModel(DUNE.id, items, tags, itemListChanges, itemChanges)

    private fun queueAll() {
        items.queue()
        tags.queue()
    }

    private fun answerAll(
        item: Result<Item> = Result.success(DUNE),
        data: Result<List<Datum>> = Result.success(listOf(AUTHOR, TITLE_PAGE)),
        itemTags: Result<List<Tag>> = Result.success(listOf(NOVEL, GERMAN, FRAKTUR))
    ) {
        items.item.removeFirst().complete(item)
        items.data.removeFirst().complete(data)
        tags.tags.removeFirst().complete(itemTags)
    }

    @Test
    fun `the load asks for the item its fields and its tags by id`() {
        queueAll()

        createSut()

        assertEquals(listOf(DUNE.id), items.itemCalls)
        assertEquals(listOf(DUNE.id), items.dataCalls)
        assertEquals(listOf(DUNE.id), tags.calls)
    }

    @Test
    fun `the screen shows loading until all three answers arrive`() {
        queueAll()
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        items.item.removeFirst().complete(Result.success(DUNE))
        items.data.removeFirst().complete(Result.success(listOf(AUTHOR)))
        assertTrue(sut.uiState.value.isLoading)

        tags.tags.removeFirst().complete(Result.success(listOf(NOVEL)))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Dune", state.name)
        assertEquals(DUNE.image, state.image)
        assertTrue(state.error.isEmpty())
    }

    @Test
    fun `fields keep the repository order and tags show their labels sorted`() {
        queueAll()
        val sut = createSut()

        answerAll(data = Result.success(listOf(AUTHOR, TITLE_PAGE)))

        assertEquals(
            listOf(
                ItemField.Text(id = "author", label = "Author", value = "Frank Herbert"),
                ItemField.Picture(id = "title-page", label = "Title page", image = PAGE_IMAGE)
            ),
            sut.uiState.value.fields
        )
        assertEquals(listOf("fraktur", "German", "Novel"), sut.uiState.value.tags)
    }

    @Test
    fun `every text-like type shows its value as text`() {
        val types = listOf(
            "text",
            "textarea",
            "country",
            "date",
            "rating",
            "number",
            "price",
            "link",
            "list",
            "choice-list",
            "checkbox"
        )
        queueAll()
        val sut = createSut()

        answerAll(data = Result.success(types.map { datum(id = it, type = it, value = "v-$it") }))

        assertEquals(
            types.map { ItemField.Text(id = it, label = "Label", value = "v-$it") },
            sut.uiState.value.fields
        )
    }

    @Test
    fun `a sign field with an image shows the image`() {
        queueAll()
        val sut = createSut()

        answerAll(data = Result.success(listOf(datum(id = "s", type = "sign", image = PAGE_IMAGE))))

        assertEquals(listOf(ItemField.Picture(id = "s", label = "Label", image = PAGE_IMAGE)), sut.uiState.value.fields)
    }

    @Test
    fun `an unknown type and an image field without an image are shown as not shown`() {
        queueAll()
        val sut = createSut()

        answerAll(
            data = Result.success(
                listOf(
                    datum(id = "v", type = "video"),
                    datum(id = "x", type = "hologram", value = "?"),
                    datum(id = "i", type = "image")
                )
            )
        )

        assertEquals(
            listOf(
                ItemField.NotShown(id = "v", label = "Label", type = "video"),
                ItemField.NotShown(id = "x", label = "Label", type = "hologram"),
                ItemField.NotShown(id = "i", label = "Label", type = "image")
            ),
            sut.uiState.value.fields
        )
    }

    @Test
    fun `a field without a label or a value shows empty text`() {
        queueAll()
        val sut = createSut()

        answerAll(data = Result.success(listOf(datum(id = "t", type = "text", label = null, value = null))))

        assertEquals(listOf(ItemField.Text(id = "t", label = "", value = "")), sut.uiState.value.fields)
    }

    @Test
    fun `a failed tag load shows its error and no partial content`() {
        queueAll()
        val sut = createSut()

        answerAll(itemTags = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(NativeText.Resource(RString.item_error_network), state.error)
        assertEquals("", state.name)
        assertNull(state.image)
        assertTrue(state.fields.isEmpty())
        assertTrue(state.tags.isEmpty())
    }

    @Test
    fun `a failed item load names the HTTP status`() {
        queueAll()
        val sut = createSut()

        answerAll(item = Result.failure(KoillectionError.Http(status = 404, message = null)))

        assertEquals(NativeText.Arguments(RString.item_error_http, listOf(404)), sut.uiState.value.error)
    }

    @Test
    fun `any other failure gets the unexpected message`() {
        queueAll()
        val sut = createSut()

        answerAll(data = Result.failure(IllegalStateException("bad json")))

        assertEquals(NativeText.Resource(RString.item_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after an error loads again and clears the error on success`() {
        queueAll()
        val sut = createSut()
        answerAll(data = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        queueAll()

        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answerAll()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Dune", sut.uiState.value.name)
        assertEquals(2, items.itemCalls.size)
    }

    @Test
    fun `retry while a load runs sends no second request`() {
        queueAll()
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, items.itemCalls.size)
        assertEquals(1, tags.calls.size)
    }

    @Test
    fun `a saved edit of this item reads it again and one of another item does not`() {
        val sut = loadedSut()

        itemChanges.notifyChanged("other-item")
        assertEquals(1, items.itemCalls.size)

        queueAll()
        itemChanges.notifyChanged(DUNE.id)
        answerAll(item = Result.success(DUNE.copy(name = "Dune Messiah")))

        assertEquals(2, items.itemCalls.size)
        assertEquals("Dune Messiah", sut.uiState.value.name)
    }

    /** A loaded item; the delete waits until the test completes [FakeItemsRepository.delete]. */
    private fun loadedSut(): ItemViewModel {
        queueAll()
        val sut = createSut()
        answerAll()
        return sut
    }

    /** The collection ids sent on [itemListChanges] while [block] runs. */
    private fun changesDuring(block: () -> Unit): List<String> {
        val changed = mutableListOf<String>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { itemListChanges.changedCollections.collect { changed += it } }
        block()
        scope.cancel()
        return changed
    }

    @Test
    fun `a loaded item can be deleted`() {
        val sut = loadedSut()

        assertTrue(sut.uiState.value.canDelete)
    }

    @Test
    fun `the delete is not offered while the item loads or after a failed load`() {
        queueAll()
        val sut = createSut()
        assertFalse(sut.uiState.value.canDelete)

        sut.uiState.value.onDelete()
        answerAll(item = Result.failure(KoillectionError.Http(status = 404, message = null)))
        assertFalse(sut.uiState.value.canDelete)
        sut.uiState.value.onDelete()

        assertTrue(items.deleteCalls.isEmpty())
    }

    @Test
    fun `a delete sends the item id and the screen waits for the answer`() {
        val sut = loadedSut()

        sut.uiState.value.onDelete()

        assertEquals(listOf(DUNE.id), items.deleteCalls)
        assertTrue(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.canDelete)
        assertFalse(sut.uiState.value.isDeleted)
    }

    @Test
    fun `a second delete while one runs sends no second request`() {
        val sut = loadedSut()

        sut.uiState.value.onDelete()
        sut.uiState.value.onDelete()

        assertEquals(1, items.deleteCalls.size)
    }

    @Test
    fun `a successful delete marks the item deleted and tells its collection`() {
        val sut = loadedSut()

        val changed = changesDuring {
            sut.uiState.value.onDelete()
            items.delete.complete(Result.success(Unit))
        }

        assertTrue(sut.uiState.value.isDeleted)
        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.deleteError.isEmpty())
        assertEquals(listOf("books"), changed)
    }

    @Test
    fun `a failed delete shows the error keeps the item and tells no collection`() {
        val sut = loadedSut()

        val changed = changesDuring {
            sut.uiState.value.onDelete()
            items.delete.complete(Result.failure(KoillectionError.Server(status = 500, message = null)))
        }

        val state = sut.uiState.value
        assertEquals(NativeText.Arguments(RString.item_error_http, listOf(500)), state.deleteError)
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals("Dune", state.name)
        assertTrue(changed.isEmpty())
    }

    @Test
    fun `an unexpected delete failure names the delete`() {
        val sut = loadedSut()

        sut.uiState.value.onDelete()
        items.delete.complete(Result.failure(IllegalStateException("bad json")))

        assertEquals(NativeText.Resource(RString.item_delete_error_unexpected), sut.uiState.value.deleteError)
    }

    @Test
    fun `after a failed delete the error can be closed and the delete tried again`() {
        val sut = loadedSut()
        sut.uiState.value.onDelete()
        items.delete.complete(Result.failure(KoillectionError.Network(IllegalStateException("offline"))))

        sut.uiState.value.onDeleteErrorShown()
        assertTrue(sut.uiState.value.deleteError.isEmpty())
        assertTrue(sut.uiState.value.canDelete)

        items.delete = CompletableDeferred(Result.success(Unit))
        sut.uiState.value.onDelete()

        assertEquals(2, items.deleteCalls.size)
        assertTrue(sut.uiState.value.isDeleted)
    }

    /** Each call takes the next queued response. Queue one with [queue] before the call. */
    private class FakeItemsRepository : ItemsRepository {
        val item = ArrayDeque<CompletableDeferred<Result<Item>>>()
        val data = ArrayDeque<CompletableDeferred<Result<List<Datum>>>>()
        val itemCalls = mutableListOf<String>()
        val dataCalls = mutableListOf<String>()
        var delete = CompletableDeferred<Result<Unit>>()
        val deleteCalls = mutableListOf<String>()

        fun queue() {
            item.addLast(CompletableDeferred())
            data.addLast(CompletableDeferred())
        }

        override suspend fun getTagItems(tagId: String): Result<List<Item>> = error("not used")

        override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

        override suspend fun getAllItems(): Result<List<Item>> = error("not used")

        override suspend fun getItem(id: String): Result<Item> {
            itemCalls += id
            return item.first().await()
        }

        override suspend fun getData(itemId: String): Result<List<Datum>> {
            dataCalls += itemId
            return data.first().await()
        }

        override suspend fun createItem(collectionId: String, name: String): Result<Item> = error("not used")

        override suspend fun createTextDatum(
            itemId: String,
            label: String,
            value: String,
            position: Int,
            type: String,
            choiceListId: String?
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

        override suspend fun deleteItem(id: String): Result<Unit> {
            deleteCalls += id
            return delete.await()
        }
    }

    private class FakeTagsRepository : TagsRepository {
        val tags = ArrayDeque<CompletableDeferred<Result<List<Tag>>>>()
        val calls = mutableListOf<String>()

        fun queue() {
            tags.addLast(CompletableDeferred())
        }

        override suspend fun getTags(): Result<List<Tag>> = error("not used")

        override suspend fun getTagCategories(): Result<List<TagCategory>> = error("not used")

        override suspend fun getItemTags(itemId: String): Result<List<Tag>> {
            calls += itemId
            return tags.first().await()
        }

        override suspend fun createTag(label: String): Result<Tag> = error("not used")

        override suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit> = error("not used")
    }

    private companion object {
        val PAGE_IMAGE = Image(
            url = "https://koi.example/uploads/u/page.jpg",
            smallThumbnailUrl = null,
            largeThumbnailUrl = "https://koi.example/uploads/u/page_large.jpg"
        )
        val DUNE = Item(
            id = "dune",
            name = "Dune",
            quantity = 1,
            collectionId = "books",
            image = Image(
                url = "https://koi.example/uploads/u/dune.jpg",
                smallThumbnailUrl = null,
                largeThumbnailUrl = "https://koi.example/uploads/u/dune_large.jpg"
            )
        )
        val AUTHOR = datum(id = "author", type = "text", label = "Author", value = "Frank Herbert")
        val TITLE_PAGE = datum(id = "title-page", type = "image", label = "Title page", image = PAGE_IMAGE)
        val NOVEL = Tag(id = "novel", label = "Novel", description = null, categoryId = null)
        val GERMAN = Tag(id = "german", label = "German", description = null, categoryId = null)
        val FRAKTUR = Tag(id = "fraktur", label = "fraktur", description = null, categoryId = null)

        fun datum(id: String, type: String, label: String? = "Label", value: String? = null, image: Image? = null) =
            Datum(id = id, type = type, label = label, value = value, position = null, image = image)
    }
}
