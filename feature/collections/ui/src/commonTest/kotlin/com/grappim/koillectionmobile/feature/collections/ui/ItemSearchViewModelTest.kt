package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_network
import com.grappim.koillectionmobile.strings.generated.resources.item_search_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemSearchViewModelTest {

    private val items = FakeItemsRepository()
    private val collections = FakeCollectionsRepository()
    private val itemListChanges = ItemListChanges()
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

    private fun createSut(handle: SavedStateHandle = savedStateHandle) =
        ItemSearchViewModel(items, collections, itemListChanges, handle)

    private fun queueAll() {
        items.queue()
        collections.queue()
    }

    private fun answerAll(
        itemList: Result<List<Item>> = Result.success(listOf(DUNE, MESSIAH, SOLARIS)),
        collectionList: Result<List<Collection>> = Result.success(listOf(BOOKS, NOVELS))
    ) {
        items.all.removeFirst().complete(itemList)
        collections.all.removeFirst().complete(collectionList)
    }

    private fun loadedSut(): ItemSearchViewModel {
        queueAll()
        val sut = createSut()
        answerAll()
        return sut
    }

    @Test
    fun `the screen shows loading until both answers arrive`() {
        queueAll()
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        items.all.removeFirst().complete(Result.success(listOf(DUNE)))
        assertTrue(sut.uiState.value.isLoading)

        collections.all.removeFirst().complete(Result.success(listOf(BOOKS)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `the load reads every item and every collection once`() {
        loadedSut()

        assertEquals(1, items.allCalls)
        assertEquals(1, collections.allCalls)
    }

    @Test
    fun `each result names its item and the title of its collection`() {
        val sut = loadedSut()

        assertEquals(
            listOf(
                ItemSearchResult(itemId = "dune", name = "Dune", collectionTitle = "Books"),
                ItemSearchResult(itemId = "messiah", name = "Dune Messiah", collectionTitle = "Novels"),
                ItemSearchResult(itemId = "solaris", name = "Solaris", collectionTitle = null)
            ),
            sut.uiState.value.results
        )
    }

    @Test
    fun `no items is empty with no error`() {
        queueAll()
        val sut = createSut()

        answerAll(itemList = Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.results.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a failed item load shows its error and no results`() {
        queueAll()
        val sut = createSut()

        answerAll(itemList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.collections_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.results.isEmpty())
    }

    @Test
    fun `a failed collection load shows its error and no results`() {
        queueAll()
        val sut = createSut()

        answerAll(collectionList = Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.collections_error_http, listOf(500)), sut.uiState.value.error)
        assertTrue(sut.uiState.value.results.isEmpty())
    }

    @Test
    fun `an unexpected failure names the items`() {
        queueAll()
        val sut = createSut()

        answerAll(itemList = Result.failure(IllegalStateException("bad json")))

        assertEquals(NativeText.Resource(RString.item_search_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after an error loads again and clears the error on success`() {
        queueAll()
        val sut = createSut()
        answerAll(itemList = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        queueAll()

        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answerAll()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(3, sut.uiState.value.results.size)
        assertEquals(2, items.allCalls)
    }

    @Test
    fun `retry while a load runs sends no second request`() {
        queueAll()
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, items.allCalls)
        assertEquals(1, collections.allCalls)
    }

    @Test
    fun `a query keeps only the results whose name contains it ignoring case`() {
        val sut = loadedSut()

        sut.uiState.value.onQueryChange("dUNE")

        assertEquals(listOf("dune", "messiah"), sut.uiState.value.visibleResults.map { it.itemId })
    }

    @Test
    fun `a query with no match keeps no result`() {
        val sut = loadedSut()

        sut.uiState.value.onQueryChange("Foundation")

        assertTrue(sut.uiState.value.visibleResults.isEmpty())
        assertEquals(3, sut.uiState.value.results.size)
    }

    @Test
    fun `an empty or blank query shows every result`() {
        val sut = loadedSut()
        sut.uiState.value.onQueryChange("Solaris")

        sut.uiState.value.onQueryChange("")
        assertEquals(3, sut.uiState.value.visibleResults.size)

        sut.uiState.value.onQueryChange("   ")
        assertEquals(3, sut.uiState.value.visibleResults.size)
    }

    @Test
    fun `a query sends no call to the server`() {
        val sut = loadedSut()

        sut.uiState.value.onQueryChange("Dune")

        assertEquals(1, items.allCalls)
        assertEquals(1, collections.allCalls)
    }

    @Test
    fun `the query survives process death`() {
        loadedSut().uiState.value.onQueryChange("solaris")
        val restored = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

        queueAll()
        val sut = createSut(restored)
        answerAll()

        assertEquals("solaris", sut.uiState.value.query)
        assertEquals(listOf("solaris"), sut.uiState.value.visibleResults.map { it.itemId })
    }

    @Test
    fun `a change to an item list loads everything again`() {
        val sut = loadedSut()
        queueAll()

        itemListChanges.notifyChanged(BOOKS.id)
        answerAll(itemList = Result.success(listOf(DUNE)))

        assertEquals(2, items.allCalls)
        assertEquals(listOf("dune"), sut.uiState.value.results.map { it.itemId })
    }

    /** Each call takes the next queued response. Queue one with [queue] before the call. */
    private class FakeItemsRepository : ItemsRepository {
        val all = ArrayDeque<CompletableDeferred<Result<List<Item>>>>()
        var allCalls = 0

        fun queue() {
            all.addLast(CompletableDeferred())
        }

        override suspend fun getAllItems(): Result<List<Item>> {
            allCalls++
            return all.first().await()
        }

        override suspend fun getTagItems(tagId: String): Result<List<Item>> = error("not used")

        override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

        override suspend fun getItem(id: String): Result<Item> = error("not used")

        override suspend fun getData(itemId: String): Result<List<Datum>> = error("not used")

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

        override suspend fun deleteItem(id: String): Result<Unit> = error("not used")
    }

    /** Each call takes the next queued response. Queue one with [queue] before the call. */
    private class FakeCollectionsRepository : CollectionsRepository {
        val all = ArrayDeque<CompletableDeferred<Result<List<Collection>>>>()
        var allCalls = 0

        fun queue() {
            all.addLast(CompletableDeferred())
        }

        override suspend fun setDefaultTemplate(id: String, templateId: String?): Result<Collection> = error("not used")

        override suspend fun getCollections(): Result<List<Collection>> {
            allCalls++
            return all.first().await()
        }

        override suspend fun getCollection(id: String): Result<Collection> = error("not used")

        override suspend fun getChildren(id: String): Result<List<Collection>> = error("not used")

        override suspend fun countItems(id: String): Result<Int> = error("not used")

        override suspend fun createCollection(title: String, parentId: String?): Result<Collection> = error("not used")

        override suspend fun renameCollection(id: String, title: String): Result<Collection> = error("not used")

        override suspend fun uploadCollectionImage(
            id: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Collection> = error("not used")

        override suspend fun deleteCollectionImage(id: String): Result<Collection> = error("not used")

        override suspend fun deleteCollection(id: String): Result<Unit> = error("not used")
    }

    private companion object {
        val BOOKS = Collection(id = "books", title = "Books", parentId = null, color = null, imageUrl = null)
        val NOVELS = Collection(id = "novels", title = "Novels", parentId = "books", color = null, imageUrl = null)
        val DUNE = Item(id = "dune", name = "Dune", quantity = 1, collectionId = "books", image = null)
        val MESSIAH = Item(id = "messiah", name = "Dune Messiah", quantity = 1, collectionId = "novels", image = null)

        /** Its collection is not in the collection list. */
        val SOLARIS = Item(id = "solaris", name = "Solaris", quantity = 1, collectionId = "gone", image = null)
    }
}
