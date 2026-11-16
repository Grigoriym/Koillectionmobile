package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionViewModelTest {

    private val collections = FakeCollectionsRepository()
    private val items = FakeItemsRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private val itemListChanges = ItemListChanges()

    private fun createSut() = CollectionViewModel(BOOKS.id, collections, items, itemListChanges)

    private fun answerAll(
        collection: Result<Collection> = Result.success(BOOKS),
        children: Result<List<Collection>> = Result.success(listOf(NOVELS)),
        itemList: Result<List<Item>> = Result.success(listOf(DUNE))
    ) {
        collections.collection.removeFirst().complete(collection)
        collections.children.removeFirst().complete(children)
        items.items.removeFirst().complete(itemList)
    }

    private fun queueAll() {
        collections.queue()
        items.queue()
    }

    @Test
    fun `the load asks for the collection its children and its items by id`() {
        queueAll()

        createSut()

        assertEquals(listOf(BOOKS.id), collections.collectionCalls)
        assertEquals(listOf(BOOKS.id), collections.childrenCalls)
        assertEquals(listOf(BOOKS.id), items.calls)
    }

    @Test
    fun `the screen shows loading until all three answers arrive`() {
        queueAll()
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        collections.collection.removeFirst().complete(Result.success(BOOKS))
        collections.children.removeFirst().complete(Result.success(listOf(NOVELS)))
        assertTrue(sut.uiState.value.isLoading)

        items.items.removeFirst().complete(Result.success(listOf(DUNE)))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Books", state.title)
        assertEquals(listOf(NOVELS), state.children)
        assertEquals(listOf(DUNE), state.items)
        assertTrue(state.error.isEmpty())
    }

    @Test
    fun `a collection with no children and no items is empty with no error`() {
        queueAll()
        val sut = createSut()

        answerAll(children = Result.success(emptyList()), itemList = Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.children.isEmpty())
        assertTrue(sut.uiState.value.items.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a failed item load shows its error and no partial content`() {
        queueAll()
        val sut = createSut()

        answerAll(itemList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(NativeText.Resource(RString.collections_error_network), state.error)
        assertTrue(state.children.isEmpty())
        assertEquals("", state.title)
    }

    @Test
    fun `a failed collection load names the HTTP status`() {
        queueAll()
        val sut = createSut()

        answerAll(collection = Result.failure(KoillectionError.Http(status = 404, message = null)))

        assertEquals(NativeText.Arguments(RString.collections_error_http, listOf(404)), sut.uiState.value.error)
    }

    @Test
    fun `retry after an error loads again and clears the error on success`() {
        queueAll()
        val sut = createSut()
        answerAll(children = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        queueAll()

        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answerAll()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(NOVELS), sut.uiState.value.children)
        assertEquals(2, items.calls.size)
    }

    @Test
    fun `retry while a load runs sends no second request`() {
        queueAll()
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, items.calls.size)
        assertEquals(1, collections.collectionCalls.size)
    }

    /** Each call takes the next queued response. Queue one with [queue] before the call. */
    private class FakeCollectionsRepository : CollectionsRepository {
        val collection = ArrayDeque<CompletableDeferred<Result<Collection>>>()
        val children = ArrayDeque<CompletableDeferred<Result<List<Collection>>>>()
        val collectionCalls = mutableListOf<String>()
        val childrenCalls = mutableListOf<String>()

        fun queue() {
            collection.addLast(CompletableDeferred())
            children.addLast(CompletableDeferred())
        }

        override suspend fun getCollections(): Result<List<Collection>> = error("not used")

        override suspend fun getCollection(id: String): Result<Collection> {
            collectionCalls += id
            return collection.first().await()
        }

        override suspend fun getChildren(id: String): Result<List<Collection>> {
            childrenCalls += id
            return children.first().await()
        }
    }

    @Test
    fun `a new item in this collection loads the collection again`() {
        queueAll()
        val sut = createSut()
        answerAll()
        queueAll()

        itemListChanges.notifyChanged(BOOKS.id)
        answerAll(itemList = Result.success(listOf(DUNE, MESSIAH)))

        assertEquals(listOf(DUNE, MESSIAH), sut.uiState.value.items)
    }

    @Test
    fun `a new item in another collection loads nothing`() {
        queueAll()
        createSut()
        answerAll()

        itemListChanges.notifyChanged("other")

        assertEquals(listOf(BOOKS.id), items.calls)
    }

    private class FakeItemsRepository : ItemsRepository {
        val items = ArrayDeque<CompletableDeferred<Result<List<Item>>>>()
        val calls = mutableListOf<String>()

        fun queue() {
            items.addLast(CompletableDeferred())
        }

        override suspend fun getItems(collectionId: String): Result<List<Item>> {
            calls += collectionId
            return items.first().await()
        }

        override suspend fun getItem(id: String): Result<Item> = error("not used")

        override suspend fun getData(itemId: String): Result<List<Datum>> = error("not used")

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
        val MESSIAH by lazy { DUNE.copy(id = "messiah", name = "Dune Messiah") }
        val BOOKS = Collection(id = "books", title = "Books", parentId = null, color = null, imageUrl = null)
        val NOVELS = Collection(id = "novels", title = "Novels", parentId = "books", color = null, imageUrl = null)
        val DUNE = Item(
            id = "dune",
            name = "Dune",
            quantity = 1,
            collectionId = "books",
            image = Image(
                url = "https://koi.example/uploads/u/dune.jpg",
                smallThumbnailUrl = "https://koi.example/uploads/u/dune_small.jpg",
                largeThumbnailUrl = null
            )
        )
    }
}
