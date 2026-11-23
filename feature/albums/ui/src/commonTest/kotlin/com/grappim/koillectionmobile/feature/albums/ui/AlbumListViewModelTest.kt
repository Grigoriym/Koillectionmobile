package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_http
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_network
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlbumListViewModelTest {

    private val repository = FakeAlbumsRepository()
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
        repository.albums.addLast(CompletableDeferred())
    }

    private fun createSut(): AlbumListViewModel {
        queue()
        return AlbumListViewModel(repository)
    }

    private fun answer(result: Result<List<Album>> = Result.success(listOf(trips, books, japan, art))) {
        repository.albums.removeFirst().complete(result)
    }

    @Test
    fun `the screen shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(1, repository.albumsCalls)
    }

    @Test
    fun `the list keeps only the top-level albums sorted by title ignoring case`() {
        val sut = createSut()

        answer()

        assertEquals(listOf(art, books, trips), sut.uiState.value.albums)
    }

    @Test
    fun `no albums is empty with no error`() {
        val sut = createSut()

        answer(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.albums.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `only child albums is empty with no error`() {
        val sut = createSut()

        answer(Result.success(listOf(japan)))

        assertTrue(sut.uiState.value.albums.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure shows its error and no albums`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.albums_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.albums.isEmpty())
    }

    @Test
    fun `an http failure shows its status`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.albums_error_http, listOf(500)), sut.uiState.value.error)
    }

    @Test
    fun `a proxy rejection shows the proxy message`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.ProxyRejection(status = 403)))

        assertEquals(NativeText.Arguments(RString.error_proxy_rejected, listOf(403)), sut.uiState.value.error)
    }

    @Test
    fun `an unexpected failure names the albums`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.albums_error_unexpected), sut.uiState.value.error)
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
        assertEquals(listOf(books), sut.uiState.value.albums)
        assertEquals(2, repository.albumsCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.albumsCalls)
    }

    private val trips = album(id = "trips", title = "Trips")
    private val books = album(id = "books", title = "books")
    private val art = album(id = "art", title = "Art")
    private val japan = album(id = "japan", title = "Japan", parentId = "trips")
}
