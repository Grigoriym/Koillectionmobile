package com.grappim.koillectionmobile.feature.inventories.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryChanges
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryCollection
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryItem
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.inventories_error_http
import com.grappim.koillectionmobile.strings.generated.resources.inventories_error_network
import com.grappim.koillectionmobile.strings.generated.resources.inventory_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryViewModelTest {

    private val repository = FakeInventoriesRepository()
    private val inventoryChanges = InventoryChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val deleted = mutableListOf<String>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { inventoryChanges.deleted.collect { deleted += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private fun queue() {
        repository.ones.addLast(CompletableDeferred())
    }

    private fun createSut(): InventoryViewModel {
        queue()
        return InventoryViewModel(INVENTORY_ID, repository, inventoryChanges)
    }

    private fun answer(result: Result<Inventory>) {
        repository.ones.removeFirst().complete(result)
    }

    private fun queueDelete() {
        repository.deletes.addLast(CompletableDeferred())
    }

    private fun answerDelete(result: Result<Unit>) {
        repository.deletes.removeFirst().complete(result)
    }

    private fun InventoryViewModel.rowKeys() = uiState.value.rows.map { it.key }

    @Test
    fun `the screen shows loading until the inventory arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)
        assertEquals(listOf(INVENTORY_ID), repository.oneCalls)

        answer(Result.success(twoRoots))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Comics check", sut.uiState.value.name)
        assertEquals(twoRoots.collections, sut.uiState.value.collections)
    }

    @Test
    fun `two root collections start closed`() {
        val sut = createSut()

        answer(Result.success(twoRoots))

        assertTrue(sut.uiState.value.expandedIds.isEmpty())
        assertEquals(listOf("collection:comics", "collection:games"), sut.rowKeys())
    }

    @Test
    fun `a lone root collection starts open and its children start closed`() {
        val sut = createSut()

        answer(Result.success(oneRoot))

        assertEquals(setOf("comics"), sut.uiState.value.expandedIds)
        assertEquals(listOf("collection:comics", "collection:marvel", "collection:dc"), sut.rowKeys())
    }

    @Test
    fun `an open collection shows its items before its children`() {
        val sut = createSut()
        answer(Result.success(twoRoots))

        sut.uiState.value.onToggle("comics")
        sut.uiState.value.onToggle("marvel")

        assertEquals(
            listOf(
                "collection:comics",
                "collection:marvel",
                "item:marvel:i1",
                "item:marvel:i2",
                "collection:dc",
                "collection:games"
            ),
            sut.rowKeys()
        )
        val rows = sut.uiState.value.rows
        assertEquals(listOf(0, 1, 2, 2, 1, 0), rows.map { it.depth })
        val items = rows.filterIsInstance<InventoryRow.Item>()
        assertEquals(listOf(true, false), items.map { it.item.checked })
    }

    @Test
    fun `a second tap closes the collection and hides what is under it`() {
        val sut = createSut()
        answer(Result.success(twoRoots))
        sut.uiState.value.onToggle("comics")
        sut.uiState.value.onToggle("marvel")

        sut.uiState.value.onToggle("comics")

        assertEquals(setOf("marvel"), sut.uiState.value.expandedIds)
        assertEquals(listOf("collection:comics", "collection:games"), sut.rowKeys())
        val header = sut.uiState.value.rows.first() as InventoryRow.Collection
        assertFalse(header.isExpanded)
    }

    @Test
    fun `a network failure shows its error and no tree`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.inventories_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.collections.isEmpty())
        assertTrue(sut.uiState.value.rows.isEmpty())
        assertFalse(sut.uiState.value.canDelete)
    }

    @Test
    fun `an unexpected failure names the inventory`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.inventory_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Http(status = 404, message = null)))
        assertEquals(NativeText.Arguments(RString.inventories_error_http, listOf(404)), sut.uiState.value.error)

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(Result.success(oneRoot))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(oneRoot.collections, sut.uiState.value.collections)
        assertEquals(listOf(INVENTORY_ID, INVENTORY_ID), repository.oneCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.oneCalls.size)
    }

    @Test
    fun `delete asks first and sends nothing until confirmed`() {
        val sut = createSut()
        answer(Result.success(twoRoots))

        sut.uiState.value.onDeleteClick()

        assertTrue(sut.uiState.value.isDeleteConfirm)
        assertTrue(repository.deleteCalls.isEmpty())

        sut.uiState.value.onDeleteDismiss()

        assertFalse(sut.uiState.value.isDeleteConfirm)
        assertTrue(repository.deleteCalls.isEmpty())
    }

    @Test
    fun `a confirmed delete leaves the screen and tells the list`() {
        val sut = createSut()
        answer(Result.success(twoRoots))

        sut.uiState.value.onDeleteClick()
        queueDelete()
        sut.uiState.value.onDeleteConfirm()

        assertFalse(sut.uiState.value.isDeleteConfirm)
        assertTrue(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.canDelete)
        assertEquals(listOf(INVENTORY_ID), repository.deleteCalls)

        answerDelete(Result.success(Unit))

        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf(INVENTORY_ID), deleted)
    }

    @Test
    fun `a failed delete stays on the screen and shows the error`() {
        val sut = createSut()
        answer(Result.success(twoRoots))

        sut.uiState.value.onDeleteClick()
        queueDelete()
        sut.uiState.value.onDeleteConfirm()
        answerDelete(Result.failure(IllegalStateException("parse")))

        assertFalse(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.isDeleted)
        assertEquals(twoRoots.collections, sut.uiState.value.collections)
        assertEquals(
            NativeText.Resource(RString.inventories_delete_error_unexpected),
            sut.uiState.value.deleteError
        )
        assertTrue(deleted.isEmpty())
        assertTrue(sut.uiState.value.canDelete)

        sut.uiState.value.onDeleteErrorShown()

        assertTrue(sut.uiState.value.deleteError.isEmpty())
    }

    @Test
    fun `no second delete starts while one runs`() {
        val sut = createSut()
        answer(Result.success(twoRoots))

        sut.uiState.value.onDeleteClick()
        queueDelete()
        sut.uiState.value.onDeleteConfirm()
        sut.uiState.value.onDeleteClick()
        sut.uiState.value.onDeleteConfirm()

        assertFalse(sut.uiState.value.isDeleteConfirm)
        assertEquals(listOf(INVENTORY_ID), repository.deleteCalls)
    }

    private companion object {
        const val INVENTORY_ID = "inv-comics"

        val marvel = node(
            id = "marvel",
            items = listOf(InventoryItem("i1", "Spider-Man #1", true), InventoryItem("i2", "Hulk #1", false)),
            total = 2,
            checked = 1
        )
        val dc = node(id = "dc", items = listOf(InventoryItem("i3", "Batman #1", false)), total = 1, checked = 0)

        // A parent the user did not pick: no items of its own, totals include the children.
        val comics = node(id = "comics", children = listOf(marvel, dc), total = 3, checked = 1)
        val games = node(id = "games", items = listOf(InventoryItem("i4", "Chess", true)), total = 1, checked = 1)

        val twoRoots = Inventory(INVENTORY_ID, "Comics check", LocalDate(2026, 11, 25), listOf(comics, games))
        val oneRoot = twoRoots.copy(collections = listOf(comics))

        fun node(
            id: String,
            items: List<InventoryItem> = emptyList(),
            children: List<InventoryCollection> = emptyList(),
            total: Int,
            checked: Int
        ) = InventoryCollection(
            id = id,
            title = "Collection $id",
            children = children,
            items = items,
            totalItems = total,
            checkedItems = checked
        )
    }
}
