package com.grappim.koillectionmobile.feature.inventories.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_http
import com.grappim.koillectionmobile.strings.generated.resources.error_network
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.inventories_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InventoryListViewModelTest {

    private val repository = FakeInventoriesRepository()
    private val inventoryChanges = InventoryChanges()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun queue() {
        repository.lists.addLast(CompletableDeferred())
    }

    private fun createSut(): InventoryListViewModel {
        queue()
        return InventoryListViewModel(repository, inventoryChanges)
    }

    private fun answer(result: Result<List<Inventory>>) {
        repository.lists.removeFirst().complete(result)
    }

    private fun queueDelete() {
        repository.deletes.addLast(CompletableDeferred())
    }

    private fun answerDelete(result: Result<Unit>) {
        repository.deletes.removeFirst().complete(result)
    }

    @Test
    fun `the list shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer(Result.success(listOf(comics)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(comics), sut.uiState.value.inventories)
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `the newest inventory comes first and the same day sorts by name`() {
        val sut = createSut()

        answer(Result.success(listOf(old, games, comics)))

        assertEquals(listOf(comics, games, old), sut.uiState.value.inventories)
    }

    @Test
    fun `the counts are the sums of the root collections`() {
        assertEquals(3, comics.checkedItems)
        assertEquals(10, comics.totalItems)
    }

    @Test
    fun `no inventories is empty with no error`() {
        val sut = createSut()

        answer(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.inventories.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure shows its error and no inventories`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.inventories.isEmpty())
    }

    @Test
    fun `an http failure shows its status`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.error_http, listOf(500)), sut.uiState.value.error)
    }

    @Test
    fun `a proxy rejection shows the proxy message`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.ProxyRejection(status = 403)))

        assertEquals(NativeText.Arguments(RString.error_proxy_rejected, listOf(403)), sut.uiState.value.error)
    }

    @Test
    fun `an unexpected failure names the inventories`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.inventories_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(Result.success(listOf(comics)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(comics), sut.uiState.value.inventories)
        assertEquals(2, repository.listCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.listCalls)
    }

    @Test
    fun `delete asks first and sends nothing until confirmed`() {
        val sut = createSut()
        answer(Result.success(listOf(comics, games)))

        sut.uiState.value.onDeleteClick(games.id)

        assertEquals(games, sut.uiState.value.deleteConfirm)
        assertTrue(repository.deleteCalls.isEmpty())

        sut.uiState.value.onDeleteDismiss()

        assertNull(sut.uiState.value.deleteConfirm)
        assertTrue(repository.deleteCalls.isEmpty())
    }

    @Test
    fun `a confirmed delete sends the delete and reads the list again`() {
        val sut = createSut()
        answer(Result.success(listOf(comics, games)))

        sut.uiState.value.onDeleteClick(games.id)
        queueDelete()
        sut.uiState.value.onDeleteConfirm()

        assertNull(sut.uiState.value.deleteConfirm)
        assertEquals(games.id, sut.uiState.value.deletingId)
        assertEquals(listOf(games.id), repository.deleteCalls)

        queue()
        answerDelete(Result.success(Unit))

        assertNull(sut.uiState.value.deletingId)
        assertTrue(sut.uiState.value.isLoading)

        answer(Result.success(listOf(comics)))

        assertEquals(listOf(comics), sut.uiState.value.inventories)
        assertEquals(2, repository.listCalls)
    }

    @Test
    fun `a failed delete keeps the list and shows the error`() {
        val sut = createSut()
        answer(Result.success(listOf(comics, games)))

        sut.uiState.value.onDeleteClick(comics.id)
        queueDelete()
        sut.uiState.value.onDeleteConfirm()
        answerDelete(Result.failure(IllegalStateException("parse")))

        assertNull(sut.uiState.value.deletingId)
        assertFalse(sut.uiState.value.isLoading)
        assertEquals(listOf(comics, games), sut.uiState.value.inventories)
        assertEquals(
            NativeText.Resource(RString.inventories_delete_error_unexpected),
            sut.uiState.value.deleteError
        )
        assertEquals(1, repository.listCalls)

        sut.uiState.value.onDeleteErrorShown()

        assertTrue(sut.uiState.value.deleteError.isEmpty())
    }

    @Test
    fun `a failed delete with an http error shows its status`() {
        val sut = createSut()
        answer(Result.success(listOf(comics)))

        sut.uiState.value.onDeleteClick(comics.id)
        queueDelete()
        sut.uiState.value.onDeleteConfirm()
        answerDelete(Result.failure(KoillectionError.Http(status = 404, message = null)))

        assertEquals(
            NativeText.Arguments(RString.error_http, listOf(404)),
            sut.uiState.value.deleteError
        )
    }

    @Test
    fun `no second delete starts while one runs`() {
        val sut = createSut()
        answer(Result.success(listOf(comics, games)))

        sut.uiState.value.onDeleteClick(comics.id)
        queueDelete()
        sut.uiState.value.onDeleteConfirm()
        sut.uiState.value.onDeleteClick(games.id)

        assertNull(sut.uiState.value.deleteConfirm)
        assertEquals(listOf(comics.id), repository.deleteCalls)
    }

    @Test
    fun `a delete on the inventory screen reads the list again`() {
        val sut = createSut()
        answer(Result.success(listOf(comics, games)))

        queue()
        inventoryChanges.notifyDeleted(games.id)

        assertTrue(sut.uiState.value.isLoading)
        assertEquals(2, repository.listCalls)

        answer(Result.success(listOf(comics)))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(listOf(comics), sut.uiState.value.inventories)
    }

    private val comics = inventory(
        id = "inv-comics",
        name = "Comics check",
        createdAt = LocalDate(2026, 11, 25),
        roots = listOf(root("c1", totalItems = 6, checkedItems = 3), root("c2", totalItems = 4, checkedItems = 0))
    )
    private val games = inventory(id = "inv-games", name = "games check", createdAt = LocalDate(2026, 11, 25))
    private val old = inventory(id = "inv-old", name = "Another", createdAt = LocalDate(2025, 1, 2))
}
