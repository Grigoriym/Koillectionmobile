package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChange
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_count_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_error_unexpected
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

class WishlistViewModelTest {

    private val repository = FakeWishlistsRepository()
    private val wishlistChanges = WishlistChanges()
    private val wishChanges = WishChanges()
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
        repository.wishlist.addLast(CompletableDeferred())
        repository.children.addLast(CompletableDeferred())
        repository.wishes.addLast(CompletableDeferred())
    }

    private fun createSut(): WishlistViewModel {
        queue()
        return WishlistViewModel(TRAVEL.id, repository, wishlistChanges, wishChanges)
    }

    private fun answer(
        wishlist: Result<Wishlist> = Result.success(TRAVEL),
        children: Result<List<Wishlist>> = Result.success(listOf(japan, asia)),
        wishes: Result<List<Wish>> = Result.success(listOf(tent, backpack, map))
    ) {
        repository.wishlist.removeFirst().complete(wishlist)
        repository.children.removeFirst().complete(children)
        repository.wishes.removeFirst().complete(wishes)
    }

    @Test
    fun `the screen shows loading until every answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Travel", sut.uiState.value.name)
        assertEquals(listOf(TRAVEL.id), repository.wishlistCalls)
        assertEquals(listOf(TRAVEL.id), repository.childrenCalls)
        assertEquals(listOf(TRAVEL.id), repository.wishesCalls)
    }

    @Test
    fun `a change to this wishlist or to a child reads again and another change does not`() {
        val sut = createSut()
        answer()

        wishlistChanges.notifyChanged("other", parentId = null)
        assertEquals(1, repository.wishlistCalls.size)

        queue()
        wishlistChanges.notifyChanged(TRAVEL.id, parentId = null)
        answer(wishlist = Result.success(TRAVEL.copy(name = "Trips")))
        assertEquals("Trips", sut.uiState.value.name)

        queue()
        wishlistChanges.notifyChanged("new", parentId = TRAVEL.id)
        answer()

        assertEquals(3, repository.wishlistCalls.size)
        assertEquals(3, repository.childrenCalls.size)
    }

    @Test
    fun `a change to a wish in this wishlist reads again and one in another wishlist does not`() {
        val sut = createSut()
        answer()

        wishChanges.notifyChanged("rail", wishlistId = "japan")
        assertEquals(1, repository.wishlistCalls.size)

        queue()
        wishChanges.notifyChanged(tent.id, wishlistId = TRAVEL.id)
        answer(wishes = Result.success(listOf(backpack, map)))

        assertEquals(listOf(backpack, map), sut.uiState.value.wishes)
        assertEquals(2, repository.wishesCalls.size)
    }

    @Test
    fun `children and wishes are sorted by name ignoring case`() {
        val sut = createSut()

        answer()

        assertEquals(listOf(asia, japan), sut.uiState.value.children)
        assertEquals(listOf(backpack, map, tent), sut.uiState.value.wishes)
    }

    @Test
    fun `no children and no wishes is empty with no error`() {
        val sut = createSut()

        answer(children = Result.success(emptyList()), wishes = Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.children.isEmpty())
        assertTrue(sut.uiState.value.wishes.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Travel", sut.uiState.value.name)
    }

    @Test
    fun `a failed wishlist read shows its error and nothing else`() {
        val sut = createSut()

        answer(wishlist = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFailedWith(sut, NativeText.Resource(RString.wishlists_error_network))
    }

    @Test
    fun `a failed children read shows its error and nothing else`() {
        val sut = createSut()

        answer(children = Result.failure(IllegalStateException("parse")))

        assertFailedWith(sut, NativeText.Resource(RString.wishlist_error_unexpected))
    }

    @Test
    fun `a failed wishes read shows its error and nothing else`() {
        val sut = createSut()

        answer(wishes = Result.failure(IllegalStateException("parse")))

        assertFailedWith(sut, NativeText.Resource(RString.wishlist_error_unexpected))
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(wishes = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Travel", sut.uiState.value.name)
        assertEquals(listOf(asia, japan), sut.uiState.value.children)
        assertEquals(listOf(backpack, map, tent), sut.uiState.value.wishes)
        assertEquals(2, repository.wishesCalls.size)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.wishlistCalls.size)
    }

    /**
     * Travel holds Japan and asia; Japan holds Tokyo. "Other" is a top-level wishlist outside the
     * tree. Travel has 3 wishes, Japan 2, asia none, Tokyo 1.
     */
    private fun answerTree() {
        repository.wishlists.addLast(
            CompletableDeferred(
                Result.success(listOf(TRAVEL, japan, asia, tokyo, wishlist(id = "other", name = "Other")))
            )
        )
        repository.wishesOf[TRAVEL.id] = Result.success(listOf(tent, backpack, map))
        repository.wishesOf[japan.id] = Result.success(listOf(wish("rail", "Rail pass"), wish("yen", "Yen")))
        repository.wishesOf[asia.id] = Result.success(emptyList())
        repository.wishesOf[tokyo.id] = Result.success(listOf(wish("sushi", "Sushi")))
    }

    /** A view model whose first load has succeeded, so the delete is offered. */
    private fun loadedSut(): WishlistViewModel = createSut().also { answer() }

    @Test
    fun `the delete counts the child wishlists at every level and the wishes in all of them`() {
        val sut = loadedSut()
        answerTree()

        sut.uiState.value.onDeleteClick()

        assertEquals(DeleteSummary(childCount = 3, wishCount = 6), sut.uiState.value.deleteSummary)
        assertFalse(sut.uiState.value.isCountingDelete)
        assertEquals(
            setOf(TRAVEL.id, japan.id, asia.id, tokyo.id),
            repository.wishesCalls.drop(1).toSet()
        )
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `the delete is not offered while the wishlist loads`() {
        val sut = createSut()

        assertFalse(sut.uiState.value.canDelete)
        sut.uiState.value.onDeleteClick()

        assertEquals(0, repository.wishlistsCalls)
    }

    @Test
    fun `a failed count shows the error and no confirm`() {
        val sut = loadedSut()
        answerTree()
        repository.wishesOf[tokyo.id] = Result.failure(IllegalStateException("parse"))

        sut.uiState.value.onDeleteClick()

        assertNull(sut.uiState.value.deleteSummary)
        assertEquals(
            NativeText.Resource(RString.wishlist_delete_count_error_unexpected),
            sut.uiState.value.deleteError
        )
        assertTrue(sut.uiState.value.canDelete)
    }

    @Test
    fun `cancel on the confirm drops it and deletes nothing`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()

        sut.uiState.value.onDeleteDismiss()

        assertNull(sut.uiState.value.deleteSummary)
        assertTrue(sut.uiState.value.canDelete)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a confirmed delete deletes the wishlist then tells its parent`() {
        val sut = createSut()
        answer(wishlist = Result.success(TRAVEL.copy(parentId = "trips")))
        answerTree()
        sut.uiState.value.onDeleteClick()
        val changes = mutableListOf<WishlistChange>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { wishlistChanges.changed.collect { changes += it } }
        val delete = CompletableDeferred<Result<Unit>>()
        repository.deleteAnswer = { delete.await() }

        sut.uiState.value.onDeleteConfirm()
        assertTrue(sut.uiState.value.isDeleting)
        assertNull(sut.uiState.value.deleteSummary)
        assertFalse(sut.uiState.value.canDelete)
        delete.complete(Result.success(Unit))

        assertEquals(listOf("delete:${TRAVEL.id}"), repository.writes)
        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf(WishlistChange(TRAVEL.id, "trips")), changes)
        // Its own signal does not read the deleted wishlist again.
        assertEquals(1, repository.wishlistCalls.size)
        scope.cancel()
    }

    @Test
    fun `a failed delete shows the error keeps the screen and tells no screen`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()
        val changes = mutableListOf<WishlistChange>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { wishlistChanges.changed.collect { changes += it } }
        repository.deleteAnswer = { Result.failure(IllegalStateException("boom")) }

        sut.uiState.value.onDeleteConfirm()

        val state = sut.uiState.value
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals(NativeText.Resource(RString.wishlist_delete_error_unexpected), state.deleteError)
        assertEquals("Travel", state.name)
        assertTrue(changes.isEmpty())

        state.onDeleteErrorShown()
        assertTrue(sut.uiState.value.deleteError.isEmpty())
        scope.cancel()
    }

    private fun assertFailedWith(sut: WishlistViewModel, error: NativeText) {
        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(error, state.error)
        assertEquals("", state.name)
        assertTrue(state.children.isEmpty())
        assertTrue(state.wishes.isEmpty())
    }

    private val japan = wishlist(id = "japan", name = "Japan", parentId = TRAVEL.id)
    private val asia = wishlist(id = "asia", name = "asia", parentId = TRAVEL.id)
    private val tent = wish(id = "tent", name = "Tent")
    private val backpack = wish(id = "backpack", name = "backpack")
    private val map = wish(id = "map", name = "Map")
    private val tokyo = wishlist(id = "tokyo", name = "Tokyo", parentId = "japan")

    private companion object {
        val TRAVEL = wishlist(id = "travel", name = "Travel")
    }
}
