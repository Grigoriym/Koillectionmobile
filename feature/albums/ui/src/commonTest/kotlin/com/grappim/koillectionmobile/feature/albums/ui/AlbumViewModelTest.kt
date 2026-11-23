package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChange
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_count_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.album_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_network
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

class AlbumViewModelTest {

    private val repository = FakeAlbumsRepository()
    private val albumChanges = AlbumChanges()
    private val photoChanges = PhotoChanges()
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
        return AlbumViewModel(TRIPS.id, repository, albumChanges, photoChanges)
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
    fun `a change to a photo of this album reads again and one in another album does not`() {
        val sut = createSut()
        answer()

        photoChanges.notifyChanged(tent.id, albumId = "elsewhere")
        assertEquals(1, repository.photosCalls.size)

        queue()
        photoChanges.notifyChanged(tent.id, albumId = TRIPS.id)
        answer(photos = Result.success(listOf(beach, map)))

        assertEquals(2, repository.photosCalls.size)
        assertEquals(listOf(beach, map), sut.uiState.value.photos)
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

    /**
     * Trips holds Japan and asia; Japan holds Tokyo. "Other" is a top-level album outside the tree.
     * Trips has 3 photos, Japan 2, asia none, Tokyo 1.
     */
    private fun answerTree() {
        repository.albums.addLast(
            CompletableDeferred(Result.success(listOf(TRIPS, japan, asia, tokyo, album(id = "other", title = "Other"))))
        )
        repository.photosOf[TRIPS.id] = Result.success(listOf(tent, beach, map))
        repository.photosOf[japan.id] = Result.success(listOf(photo("temple", "Temple"), photo("fuji", "Fuji")))
        repository.photosOf[asia.id] = Result.success(emptyList())
        repository.photosOf[tokyo.id] = Result.success(listOf(photo("tower", "Tower")))
    }

    /** A view model whose first load has succeeded, so the delete is offered. */
    private fun loadedSut(): AlbumViewModel = createSut().also { answer() }

    @Test
    fun `the delete counts the child albums at every level and the photos in all of them`() {
        val sut = loadedSut()
        answerTree()

        sut.uiState.value.onDeleteClick()

        assertEquals(DeleteSummary(childCount = 3, photoCount = 6), sut.uiState.value.deleteSummary)
        assertFalse(sut.uiState.value.isCountingDelete)
        assertEquals(
            setOf(TRIPS.id, japan.id, asia.id, tokyo.id),
            repository.photosCalls.drop(1).toSet()
        )
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `the delete is not offered while the album loads`() {
        val sut = createSut()

        assertFalse(sut.uiState.value.canDelete)
        sut.uiState.value.onDeleteClick()

        assertEquals(0, repository.albumsCalls)
    }

    @Test
    fun `a failed count shows the error and no confirm`() {
        val sut = loadedSut()
        answerTree()
        repository.photosOf[tokyo.id] = Result.failure(IllegalStateException("parse"))

        sut.uiState.value.onDeleteClick()

        assertNull(sut.uiState.value.deleteSummary)
        assertEquals(
            NativeText.Resource(RString.album_delete_count_error_unexpected),
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
    fun `a confirmed delete deletes the album then tells its parent`() {
        val sut = createSut()
        answer(album = Result.success(TRIPS.copy(parentId = "travel")))
        answerTree()
        sut.uiState.value.onDeleteClick()
        val changes = mutableListOf<AlbumChange>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { albumChanges.changed.collect { changes += it } }
        val delete = CompletableDeferred<Result<Unit>>()
        repository.deleteAnswer = { delete.await() }

        sut.uiState.value.onDeleteConfirm()
        assertTrue(sut.uiState.value.isDeleting)
        assertNull(sut.uiState.value.deleteSummary)
        assertFalse(sut.uiState.value.canDelete)
        delete.complete(Result.success(Unit))

        assertEquals(listOf("delete:${TRIPS.id}"), repository.writes)
        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf(AlbumChange(TRIPS.id, "travel")), changes)
        // Its own signal does not read the deleted album again.
        assertEquals(1, repository.albumCalls.size)
        scope.cancel()
    }

    @Test
    fun `a failed delete shows the error keeps the screen and tells no screen`() {
        val sut = loadedSut()
        answerTree()
        sut.uiState.value.onDeleteClick()
        val changes = mutableListOf<AlbumChange>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { albumChanges.changed.collect { changes += it } }
        repository.deleteAnswer = { Result.failure(IllegalStateException("boom")) }

        sut.uiState.value.onDeleteConfirm()

        val state = sut.uiState.value
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals(NativeText.Resource(RString.album_delete_error_unexpected), state.deleteError)
        assertEquals("Trips", state.title)
        assertTrue(changes.isEmpty())

        state.onDeleteErrorShown()
        assertTrue(sut.uiState.value.deleteError.isEmpty())
        scope.cancel()
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
    private val tokyo = album(id = "tokyo", title = "Tokyo", parentId = "japan")

    private companion object {
        val TRIPS = album(id = "trips", title = "Trips")
    }
}
