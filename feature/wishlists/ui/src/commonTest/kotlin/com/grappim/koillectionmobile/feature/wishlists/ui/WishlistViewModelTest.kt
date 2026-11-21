package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WishlistViewModelTest {

    private val repository = FakeWishlistsRepository()
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
        return WishlistViewModel(TRAVEL.id, repository)
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

    private companion object {
        val TRAVEL = wishlist(id = "travel", name = "Travel")
    }
}
