package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_count_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_network
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
    private val collectionListChanges = CollectionListChanges()
    private val collectionChanges = CollectionChanges()

    private val savedStateHandle = SavedStateHandle()

    private fun createSut(handle: SavedStateHandle = savedStateHandle) = CollectionViewModel(
        BOOKS.id,
        collections,
        items,
        itemListChanges,
        collectionListChanges,
        collectionChanges,
        handle
    )

    /** A view model whose first load has succeeded, so the delete is offered. */
    private fun loadedSut(itemList: List<Item> = listOf(DUNE)): CollectionViewModel {
        queueAll()
        val sut = createSut()
        answerAll(itemList = Result.success(itemList))
        return sut
    }

    /** BOOKS holds NOVELS, which holds SCI_FI. GAMES and its child RPG are not under BOOKS. */
    private fun answerTree() {
        collections.all = Result.success(listOf(BOOKS, NOVELS, GAMES, SCI_FI, RPG))
        collections.counts.putAll(
            mapOf(
                BOOKS.id to Result.success(3),
                NOVELS.id to Result.success(40),
                SCI_FI.id to Result.success(500)
            )
        )
    }

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
    fun `a query keeps only the items whose name contains it`() {
        val sut = loadedSut(listOf(DUNE, MESSIAH, SOLARIS))

        sut.uiState.value.onQueryChange("Messiah")

        assertEquals(listOf(MESSIAH), sut.uiState.value.visibleItems)
        assertEquals(listOf(DUNE, MESSIAH, SOLARIS), sut.uiState.value.items)
    }

    @Test
    fun `the query ignores case`() {
        val sut = loadedSut(listOf(DUNE, MESSIAH, SOLARIS))

        sut.uiState.value.onQueryChange("dUNE")

        assertEquals(listOf(DUNE, MESSIAH), sut.uiState.value.visibleItems)
    }

    @Test
    fun `a query with no match keeps no item`() {
        val sut = loadedSut(listOf(DUNE, MESSIAH, SOLARIS))

        sut.uiState.value.onQueryChange("Foundation")

        assertTrue(sut.uiState.value.visibleItems.isEmpty())
        assertEquals(listOf(DUNE, MESSIAH, SOLARIS), sut.uiState.value.items)
    }

    @Test
    fun `an empty or blank query shows every item`() {
        val sut = loadedSut(listOf(DUNE, MESSIAH, SOLARIS))
        sut.uiState.value.onQueryChange("Solaris")

        sut.uiState.value.onQueryChange("")
        assertEquals(listOf(DUNE, MESSIAH, SOLARIS), sut.uiState.value.visibleItems)

        sut.uiState.value.onQueryChange("   ")
        assertEquals(listOf(DUNE, MESSIAH, SOLARIS), sut.uiState.value.visibleItems)
    }

    @Test
    fun `a query sends no call to the server`() {
        val sut = loadedSut(listOf(DUNE, MESSIAH, SOLARIS))

        sut.uiState.value.onQueryChange("Dune")

        assertEquals(listOf(BOOKS.id), collections.collectionCalls)
        assertEquals(listOf(BOOKS.id), collections.childrenCalls)
        assertEquals(listOf(BOOKS.id), items.calls)
    }

    @Test
    fun `the query survives process death`() {
        loadedSut(listOf(DUNE, MESSIAH, SOLARIS)).uiState.value.onQueryChange("solaris")
        val restored = SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

        queueAll()
        val sut = createSut(restored)
        answerAll(itemList = Result.success(listOf(DUNE, MESSIAH, SOLARIS)))

        assertEquals("solaris", sut.uiState.value.query)
        assertEquals(listOf(SOLARIS), sut.uiState.value.visibleItems)
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
        var all: Result<List<Collection>> = Result.success(emptyList())
        var allCalls = 0
        val counts = mutableMapOf<String, Result<Int>>()
        val countCalls = mutableListOf<String>()
        var delete = CompletableDeferred<Result<Unit>>()
        val deleteCalls = mutableListOf<String>()

        fun queue() {
            collection.addLast(CompletableDeferred())
            children.addLast(CompletableDeferred())
        }

        override suspend fun getCollections(): Result<List<Collection>> {
            allCalls++
            return all
        }

        override suspend fun getCollection(id: String): Result<Collection> {
            collectionCalls += id
            return collection.first().await()
        }

        override suspend fun getChildren(id: String): Result<List<Collection>> {
            childrenCalls += id
            return children.first().await()
        }

        override suspend fun countItems(id: String): Result<Int> {
            countCalls += id
            return counts.getValue(id)
        }

        override suspend fun createCollection(title: String, parentId: String?): Result<Collection> = error("not used")

        override suspend fun renameCollection(id: String, title: String): Result<Collection> = error("not used")

        override suspend fun uploadCollectionImage(
            id: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Collection> = error("not used")

        override suspend fun deleteCollectionImage(id: String): Result<Collection> = error("not used")

        override suspend fun deleteCollection(id: String): Result<Unit> {
            deleteCalls += id
            return delete.await()
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

    @Test
    fun `the delete counts the child collections at every level and the items in all of them`() {
        val sut = loadedSut()
        answerTree()

        sut.uiState.value.onDeleteClick()

        assertEquals(DeleteSummary(childCount = 2, itemCount = 543), sut.uiState.value.deleteSummary)
        assertEquals(setOf(BOOKS.id, NOVELS.id, SCI_FI.id), collections.countCalls.toSet())
        assertEquals(3, collections.countCalls.size)
        assertFalse(sut.uiState.value.isCountingDelete)
        assertTrue(collections.deleteCalls.isEmpty())
    }

    @Test
    fun `a collection without children counts only its own items`() {
        val sut = loadedSut()
        collections.all = Result.success(listOf(BOOKS, GAMES))
        collections.counts[BOOKS.id] = Result.success(0)

        sut.uiState.value.onDeleteClick()

        assertEquals(DeleteSummary(childCount = 0, itemCount = 0), sut.uiState.value.deleteSummary)
    }

    @Test
    fun `the delete is not offered while the collection loads`() {
        queueAll()
        val sut = createSut()

        assertFalse(sut.uiState.value.canDelete)
        sut.uiState.value.onDeleteClick()

        assertEquals(0, collections.allCalls)
    }

    @Test
    fun `a failed collection list shows the error and no confirm`() {
        val sut = loadedSut()
        collections.all = Result.failure(KoillectionError.Network(IllegalStateException("offline")))

        sut.uiState.value.onDeleteClick()

        assertNull(sut.uiState.value.deleteSummary)
        assertEquals(NativeText.Resource(RString.collections_error_network), sut.uiState.value.deleteError)
        assertTrue(sut.uiState.value.canDelete)
    }

    @Test
    fun `a failed item count shows the count error and no confirm`() {
        val sut = loadedSut()
        answerTree()
        collections.counts[SCI_FI.id] = Result.failure(IllegalStateException("bad page"))

        sut.uiState.value.onDeleteClick()

        assertNull(sut.uiState.value.deleteSummary)
        assertEquals(
            NativeText.Resource(RString.collection_delete_count_error_unexpected),
            sut.uiState.value.deleteError
        )
    }

    @Test
    fun `cancel on the confirm drops it and deletes nothing`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteDismiss()

        assertNull(sut.uiState.value.deleteSummary)
        assertTrue(sut.uiState.value.canDelete)
        assertTrue(collections.deleteCalls.isEmpty())
    }

    @Test
    fun `a confirmed delete deletes the collection then tells its parent list`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()
        val parents = mutableListOf<String?>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { collectionListChanges.changedParents.collect { parents += it } }

        sut.uiState.value.onDeleteConfirm()
        assertTrue(sut.uiState.value.isDeleting)
        assertNull(sut.uiState.value.deleteSummary)
        assertFalse(sut.uiState.value.canDelete)
        collections.delete.complete(Result.success(Unit))

        assertEquals(listOf(BOOKS.id), collections.deleteCalls)
        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf<String?>(BOOKS.parentId), parents)
        scope.cancel()
    }

    @Test
    fun `a failed delete shows the error keeps the screen and tells no list`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()
        val parents = mutableListOf<String?>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { collectionListChanges.changedParents.collect { parents += it } }

        sut.uiState.value.onDeleteConfirm()
        collections.delete.complete(Result.failure(IllegalStateException("boom")))

        val state = sut.uiState.value
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals(NativeText.Resource(RString.collection_delete_error_unexpected), state.deleteError)
        assertEquals("Books", state.title)
        assertTrue(parents.isEmpty())

        state.onDeleteErrorShown()
        assertTrue(sut.uiState.value.deleteError.isEmpty())
        scope.cancel()
    }

    @Test
    fun `a failed delete with an HTTP status names the status`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteConfirm()
        collections.delete.complete(Result.failure(KoillectionError.Http(status = 403, message = null)))

        assertEquals(NativeText.Arguments(RString.collections_error_http, listOf(403)), sut.uiState.value.deleteError)
    }

    @Test
    fun `a change to this collection's children loads the collection again`() {
        val sut = loadedSut()
        queueAll()

        collectionListChanges.notifyChanged(BOOKS.id)
        answerAll(children = Result.success(emptyList()))

        assertTrue(sut.uiState.value.children.isEmpty())
        assertEquals(2, collections.childrenCalls.size)
    }

    @Test
    fun `a change to another collection's children loads nothing`() {
        loadedSut()

        collectionListChanges.notifyChanged(null)
        collectionListChanges.notifyChanged(GAMES.id)

        assertEquals(1, collections.childrenCalls.size)
    }

    @Test
    fun `a saved edit of this collection loads its new title`() {
        val sut = loadedSut()
        queueAll()

        collectionChanges.notifyChanged(BOOKS.id)
        answerAll(collection = Result.success(BOOKS.copy(title = "Old books")))

        assertEquals("Old books", sut.uiState.value.title)
        assertEquals(2, collections.collectionCalls.size)
    }

    @Test
    fun `a saved edit of another collection loads nothing`() {
        loadedSut()

        collectionChanges.notifyChanged(GAMES.id)

        assertEquals(1, collections.collectionCalls.size)
    }

    private class FakeItemsRepository : ItemsRepository {
        val items = ArrayDeque<CompletableDeferred<Result<List<Item>>>>()
        val calls = mutableListOf<String>()

        fun queue() {
            items.addLast(CompletableDeferred())
        }

        override suspend fun getTagItems(tagId: String): Result<List<Item>> = error("not used")

        override suspend fun getItems(collectionId: String): Result<List<Item>> {
            calls += collectionId
            return items.first().await()
        }

        override suspend fun getAllItems(): Result<List<Item>> = error("not used")

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
        val SOLARIS by lazy { DUNE.copy(id = "solaris", name = "Solaris") }
        val BOOKS = Collection(id = "books", title = "Books", parentId = null, color = null, imageUrl = null)
        val NOVELS = Collection(id = "novels", title = "Novels", parentId = "books", color = null, imageUrl = null)
        val SCI_FI = Collection(id = "sci-fi", title = "Sci-fi", parentId = "novels", color = null, imageUrl = null)
        val GAMES = Collection(id = "games", title = "Games", parentId = null, color = null, imageUrl = null)
        val RPG = Collection(id = "rpg", title = "RPG", parentId = "games", color = null, imageUrl = null)
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
