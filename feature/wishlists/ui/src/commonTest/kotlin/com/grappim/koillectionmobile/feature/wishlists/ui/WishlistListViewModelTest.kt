package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_error_http
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_error_network
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WishlistListViewModelTest {

    private val repository = FakeWishlistsRepository()
    private val wishlistChanges = WishlistChanges()
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
        repository.wishlists.addLast(CompletableDeferred())
    }

    private fun createSut(): WishlistListViewModel {
        queue()
        return WishlistListViewModel(repository, wishlistChanges)
    }

    private fun answer(result: Result<List<Wishlist>> = Result.success(listOf(travel, books, japan, art))) {
        repository.wishlists.removeFirst().complete(result)
    }

    @Test
    fun `the screen shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(1, repository.wishlistsCalls)
    }

    @Test
    fun `a change to a top-level wishlist reads the list again and a child change does not`() {
        val sut = createSut()
        answer()

        wishlistChanges.notifyChanged(japan.id, parentId = travel.id)
        assertEquals(1, repository.wishlistsCalls)

        queue()
        wishlistChanges.notifyChanged("new", parentId = null)
        answer(Result.success(listOf(travel, wishlist(id = "new", name = "New"))))

        assertEquals(2, repository.wishlistsCalls)
        assertEquals(listOf("new", travel.id), sut.uiState.value.wishlists.map { it.id })
    }

    @Test
    fun `the list keeps only the top-level wishlists sorted by name ignoring case`() {
        val sut = createSut()

        answer()

        assertEquals(listOf(art, books, travel), sut.uiState.value.wishlists)
    }

    @Test
    fun `no wishlists is empty with no error`() {
        val sut = createSut()

        answer(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.wishlists.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `only child wishlists is empty with no error`() {
        val sut = createSut()

        answer(Result.success(listOf(japan)))

        assertTrue(sut.uiState.value.wishlists.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure shows its error and no wishlists`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.wishlists_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.wishlists.isEmpty())
    }

    @Test
    fun `an http failure shows its status`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.wishlists_error_http, listOf(500)), sut.uiState.value.error)
    }

    @Test
    fun `a proxy rejection shows the proxy message`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.ProxyRejection(status = 403)))

        assertEquals(NativeText.Arguments(RString.error_proxy_rejected, listOf(403)), sut.uiState.value.error)
    }

    @Test
    fun `an unexpected failure names the wishlists`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.wishlists_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(Result.success(listOf(books)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(books), sut.uiState.value.wishlists)
        assertEquals(2, repository.wishlistsCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.wishlistsCalls)
    }

    private val travel = wishlist(id = "travel", name = "Travel")
    private val books = wishlist(id = "books", name = "books")
    private val art = wishlist(id = "art", name = "Art")
    private val japan = wishlist(id = "japan", name = "Japan", parentId = "travel")
}
