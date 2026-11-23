package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlbumViewModelTest {

    private val repository = FakeAlbumsRepository()
    private val albumChanges = AlbumChanges()
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
        repository.album.addLast(CompletableDeferred())
        repository.children.addLast(CompletableDeferred())
        repository.photos.addLast(CompletableDeferred())
    }

    private fun createSut(): AlbumViewModel {
        queue()
        return AlbumViewModel(TRIPS.id, repository, albumChanges)
    }

    private fun answer(
        album: Result<Album> = Result.success(TRIPS),
        children: Result<List<Album>> = Result.success(listOf(japan, asia)),
        photos: Result<List<Photo>> = Result.success(listOf(tent, beach, map))
    ) {
        repository.album.removeFirst().complete(album)
        repository.children.removeFirst().complete(children)
        repository.photos.removeFirst().complete(photos)
    }

    @Test
    fun `the screen shows loading until every answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Trips", sut.uiState.value.title)
        assertEquals(listOf(TRIPS.id), repository.albumCalls)
        assertEquals(listOf(TRIPS.id), repository.childrenCalls)
        assertEquals(listOf(TRIPS.id), repository.photosCalls)
    }

    @Test
    fun `a change to this album or a child reads again and another change does not`() {
        val sut = createSut()
        answer()

        albumChanges.notifyChanged("other", parentId = "elsewhere")
        assertEquals(1, repository.albumCalls.size)

        queue()
        albumChanges.notifyChanged(TRIPS.id, parentId = null)
        answer(album = Result.success(TRIPS.copy(title = "Travel")))
        assertEquals("Travel", sut.uiState.value.title)

        queue()
        albumChanges.notifyChanged("new", parentId = TRIPS.id)
        answer(children = Result.success(listOf(japan, asia, album(id = "new", title = "New", parentId = TRIPS.id))))

        assertEquals(3, repository.albumCalls.size)
        assertEquals(listOf("asia", "japan", "new"), sut.uiState.value.children.map { it.id })
    }

    @Test
    fun `children and photos are sorted by title ignoring case`() {
        val sut = createSut()

        answer()

        assertEquals(listOf(asia, japan), sut.uiState.value.children)
        assertEquals(listOf(beach, map, tent), sut.uiState.value.photos)
    }

    @Test
    fun `no children and no photos is empty with no error`() {
        val sut = createSut()

        answer(children = Result.success(emptyList()), photos = Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.children.isEmpty())
        assertTrue(sut.uiState.value.photos.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Trips", sut.uiState.value.title)
    }

    @Test
    fun `a failed album read shows its error and nothing else`() {
        val sut = createSut()

        answer(album = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFailedWith(sut, NativeText.Resource(RString.albums_error_network))
    }

    @Test
    fun `a failed children read shows its error and nothing else`() {
        val sut = createSut()

        answer(children = Result.failure(IllegalStateException("parse")))

        assertFailedWith(sut, NativeText.Resource(RString.album_error_unexpected))
    }

    @Test
    fun `a failed photos read shows its error and nothing else`() {
        val sut = createSut()

        answer(photos = Result.failure(IllegalStateException("parse")))

        assertFailedWith(sut, NativeText.Resource(RString.album_error_unexpected))
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(photos = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Trips", sut.uiState.value.title)
        assertEquals(listOf(asia, japan), sut.uiState.value.children)
        assertEquals(listOf(beach, map, tent), sut.uiState.value.photos)
        assertEquals(2, repository.photosCalls.size)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.albumCalls.size)
    }

    private fun assertFailedWith(sut: AlbumViewModel, error: NativeText) {
        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(error, state.error)
        assertEquals("", state.title)
        assertTrue(state.children.isEmpty())
        assertTrue(state.photos.isEmpty())
    }

    private val japan = album(id = "japan", title = "Japan", parentId = TRIPS.id)
    private val asia = album(id = "asia", title = "asia", parentId = TRIPS.id)
    private val tent = photo(id = "tent", title = "Tent")
    private val beach = photo(id = "beach", title = "beach")
    private val map = photo(id = "map", title = "Map")

    private companion object {
        val TRIPS = album(id = "trips", title = "Trips")
    }
}
