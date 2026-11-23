package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChange
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_network
import com.grappim.koillectionmobile.strings.generated.resources.photo_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.photo_error_unexpected
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoViewModelTest {

    private val repository = FakeAlbumsRepository()
    private val photoChanges = PhotoChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val changes = mutableListOf<PhotoChange>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { photoChanges.changed.collect { changes += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private fun createSut(): PhotoViewModel {
        repository.photo.addLast(CompletableDeferred())
        return PhotoViewModel(BEACH.id, repository, photoChanges)
    }

    private fun answer(result: Result<Photo> = Result.success(BEACH)) {
        repository.photo.removeFirst().complete(result)
    }

    /** A view model whose first load has succeeded, so the delete is offered. */
    private fun loadedSut(): PhotoViewModel = createSut().also { answer() }

    @Test
    fun `the screen shows loading until the photo arrives then shows it`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)
        assertFalse(sut.uiState.value.canDelete)

        answer()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error.isEmpty())
        assertEquals(BEACH, state.photo)
        assertTrue(state.canDelete)
        assertEquals(listOf(BEACH.id), repository.photoCalls)
    }

    @Test
    fun `a failed read shows its error and no photo`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.photo)
        assertEquals(NativeText.Resource(RString.albums_error_network), state.error)
        assertFalse(state.canDelete)
    }

    @Test
    fun `retry after a failure reads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(IllegalStateException("parse")))
        assertEquals(NativeText.Resource(RString.photo_error_unexpected), sut.uiState.value.error)

        repository.photo.addLast(CompletableDeferred())
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(BEACH, sut.uiState.value.photo)
        assertEquals(2, repository.photoCalls.size)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.photoCalls.size)
    }

    @Test
    fun `the delete is not offered while the photo loads`() {
        val sut = createSut()

        sut.uiState.value.onDelete()

        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a delete deletes the photo then tells its album`() {
        val sut = loadedSut()
        val delete = CompletableDeferred<Result<Unit>>()
        repository.deletePhotoAnswer = { delete.await() }

        sut.uiState.value.onDelete()
        assertTrue(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.canDelete)
        sut.uiState.value.onDelete()
        delete.complete(Result.success(Unit))

        assertEquals(listOf("deletePhoto:${BEACH.id}"), repository.writes)
        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf(PhotoChange(BEACH.id, BEACH.albumId)), changes)
    }

    @Test
    fun `a failed delete shows the error keeps the screen and tells no screen`() {
        val sut = loadedSut()
        repository.deletePhotoAnswer = { Result.failure(IllegalStateException("boom")) }

        sut.uiState.value.onDelete()

        val state = sut.uiState.value
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals(NativeText.Resource(RString.photo_delete_error_unexpected), state.deleteError)
        assertEquals(BEACH, state.photo)
        assertTrue(state.canDelete)
        assertTrue(changes.isEmpty())

        state.onDeleteErrorShown()
        assertTrue(sut.uiState.value.deleteError.isEmpty())
    }

    private companion object {
        val BEACH = photo(id = "beach", title = "Beach").copy(
            comment = "Low tide",
            place = "Brittany",
            imageUrl = "https://example.com/beach.jpg",
            takenAt = LocalDate(2024, 7, 14)
        )
    }
}
