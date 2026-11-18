package com.grappim.koillectionmobile.feature.tags.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.tag_items_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.tags_error_http
import com.grappim.koillectionmobile.strings.generated.resources.tags_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagItemsViewModelTest {

    private val items = FakeItemsRepository()
    private val itemListChanges = ItemListChanges()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun createSut(): TagItemsViewModel {
        items.queue()
        return TagItemsViewModel(TAG_ID, items, itemListChanges)
    }

    private fun answer(result: Result<List<Item>> = Result.success(listOf(dune, solaris))) {
        items.tagItems.removeFirst().complete(result)
    }

    @Test
    fun `the screen shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `the load reads the items of the tag once`() {
        createSut()
        answer()

        assertEquals(listOf(TAG_ID), items.tagItemCalls)
    }

    @Test
    fun `each row names its item in the server's order`() {
        val sut = createSut()

        answer(Result.success(listOf(solaris, dune)))

        assertEquals(
            listOf(TagItem(itemId = "solaris", name = "Solaris"), TagItem(itemId = "dune", name = "Dune")),
            sut.uiState.value.items
        )
    }

    @Test
    fun `no items is empty with no error`() {
        val sut = createSut()

        answer(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.items.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure shows its error and no items`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.tags_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.items.isEmpty())
    }

    @Test
    fun `an http failure shows its status`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Server(status = 503, message = null)))

        assertEquals(NativeText.Arguments(RString.tags_error_http, listOf(503)), sut.uiState.value.error)
    }

    @Test
    fun `an unexpected failure names the items of the tag`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.tag_items_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        items.queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(Result.success(listOf(dune)))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(listOf(TagItem(itemId = "dune", name = "Dune")), sut.uiState.value.items)
        assertEquals(listOf(TAG_ID, TAG_ID), items.tagItemCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, items.tagItemCalls.size)
    }

    @Test
    fun `a change to any item list reads the items again`() {
        val sut = createSut()
        answer()

        items.queue()
        itemListChanges.notifyChanged("any-collection")
        answer(Result.success(listOf(dune)))

        assertEquals(2, items.tagItemCalls.size)
        assertEquals(listOf("dune"), sut.uiState.value.items.map { it.itemId })
    }

    private val dune = Item(id = "dune", name = "Dune", quantity = 1, collectionId = "books", image = null)
    private val solaris = Item(id = "solaris", name = "Solaris", quantity = 1, collectionId = null, image = null)

    /** Each call takes the first queued response. Queue one with [queue] before the call. */
    private class FakeItemsRepository : ItemsRepository {
        val tagItems = ArrayDeque<CompletableDeferred<Result<List<Item>>>>()
        val tagItemCalls = mutableListOf<String>()

        fun queue() {
            tagItems.addLast(CompletableDeferred())
        }

        override suspend fun getTagItems(tagId: String): Result<List<Item>> {
            tagItemCalls += tagId
            return tagItems.first().await()
        }

        override suspend fun getAllItems(): Result<List<Item>> = error("not used")

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

    private companion object {
        const val TAG_ID = "fiction"
    }
}
