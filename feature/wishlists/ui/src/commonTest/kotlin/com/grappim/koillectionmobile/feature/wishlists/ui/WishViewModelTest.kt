package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChange
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wish_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wish_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_error_network
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

class WishViewModelTest {

    private val repository = FakeWishlistsRepository()
    private val wishChanges = WishChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val changes = mutableListOf<WishChange>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { wishChanges.changed.collect { changes += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private fun createSut(): WishViewModel {
        repository.wish.addLast(CompletableDeferred())
        return WishViewModel(TENT.id, repository, wishChanges)
    }

    private fun answer(result: Result<Wish> = Result.success(TENT)) {
        repository.wish.removeFirst().complete(result)
    }

    /** A view model whose first load has succeeded, so the delete is offered. */
    private fun loadedSut(): WishViewModel = createSut().also { answer() }

    @Test
    fun `the screen shows loading until the wish arrives then shows it`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)
        assertFalse(sut.uiState.value.canDelete)

        answer()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error.isEmpty())
        assertEquals(TENT, state.wish)
        assertTrue(state.canDelete)
        assertEquals(listOf(TENT.id), repository.wishCalls)
    }

    @Test
    fun `a failed read shows its error and no wish`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.wish)
        assertEquals(NativeText.Resource(RString.wishlists_error_network), state.error)
        assertFalse(state.canDelete)
    }

    @Test
    fun `retry after a failure reads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(IllegalStateException("parse")))
        assertEquals(NativeText.Resource(RString.wish_error_unexpected), sut.uiState.value.error)

        repository.wish.addLast(CompletableDeferred())
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(TENT, sut.uiState.value.wish)
        assertEquals(2, repository.wishCalls.size)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.wishCalls.size)
    }

    @Test
    fun `the delete is not offered while the wish loads`() {
        val sut = createSut()

        sut.uiState.value.onDelete()

        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a delete deletes the wish then tells its wishlist`() {
        val sut = loadedSut()
        val delete = CompletableDeferred<Result<Unit>>()
        repository.deleteWishAnswer = { delete.await() }

        sut.uiState.value.onDelete()
        assertTrue(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.canDelete)
        sut.uiState.value.onDelete()
        delete.complete(Result.success(Unit))

        assertEquals(listOf("deleteWish:${TENT.id}"), repository.writes)
        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf(WishChange(TENT.id, TENT.wishlistId)), changes)
    }

    @Test
    fun `a failed delete shows the error keeps the screen and tells no screen`() {
        val sut = loadedSut()
        repository.deleteWishAnswer = { Result.failure(IllegalStateException("boom")) }

        sut.uiState.value.onDelete()

        val state = sut.uiState.value
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals(NativeText.Resource(RString.wish_delete_error_unexpected), state.deleteError)
        assertEquals(TENT, state.wish)
        assertTrue(state.canDelete)
        assertTrue(changes.isEmpty())

        state.onDeleteErrorShown()
        assertTrue(sut.uiState.value.deleteError.isEmpty())
    }

    private companion object {
        val TENT = wish(id = "tent", name = "Tent").copy(url = "https://example.com/tent", comment = "Two people")
    }
}
