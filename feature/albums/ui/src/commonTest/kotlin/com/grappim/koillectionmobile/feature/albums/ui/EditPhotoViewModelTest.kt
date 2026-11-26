package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChange
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.error_network
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

class EditPhotoViewModelTest {

    private val repository = FakeAlbumsRepository()
    private val photoFiles = FakePhotoFiles()
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

    private val savedStateHandle = SavedStateHandle()

    /** The photo read answers [loaded] at once. */
    private fun createSut(
        handle: SavedStateHandle = savedStateHandle,
        loaded: Result<Photo> = Result.success(BEACH)
    ): EditPhotoViewModel {
        repository.photo.clear()
        repository.photo.addLast(CompletableDeferred(loaded))
        return EditPhotoViewModel(BEACH.id, repository, photoFiles, photoChanges, handle)
    }

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    private fun EditPhotoViewModel.type(field: PhotoField, text: String) = uiState.value.onFieldChange(field, text)

    @Test
    fun `the form is filled from the server and an unchanged form sends no call`() {
        val sut = createSut()

        val state = sut.uiState.value
        assertEquals(PhotoFormValues(title = "Beach", comment = "Low tide", place = "Brittany"), state.values)
        assertEquals(IMAGE_URL, state.original?.imageUrl)
        assertFalse(state.canSave)
        state.onSave()

        assertNull(sut.uiState.value.save)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `each changed field alone sends one patch with only that key then tells the screens`() {
        val edits = mapOf(
            PhotoField.TITLE to "Beach at noon",
            PhotoField.COMMENT to "High tide",
            PhotoField.PLACE to "Normandy"
        )
        edits.forEach { (field, text) ->
            repository.writes.clear()
            repository.photoEdits.clear()
            changes.clear()
            val sut = createSut(handle = SavedStateHandle())
            sut.type(field, text)

            sut.uiState.value.onSave()

            assertEquals(listOf("editPhoto:beach"), repository.writes, field.name)
            assertEquals(listOf(mapOf<PhotoField, String?>(field to text)), repository.photoEdits, field.name)
            assertTrue(sut.uiState.value.isSaved, field.name)
            assertEquals(listOf(PhotoChange(BEACH.id, ALBUM_ID)), changes, field.name)
        }
    }

    @Test
    fun `a cleared field is sent as null`() {
        val sut = createSut()
        sut.type(PhotoField.COMMENT, "")

        sut.uiState.value.onSave()

        assertEquals(listOf(mapOf<PhotoField, String?>(PhotoField.COMMENT to null)), repository.photoEdits)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a blank title cannot be saved`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, " ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a value typed back to the loaded one is no change`() {
        val sut = createSut()
        sut.type(PhotoField.PLACE, "Normandy")
        sut.type(PhotoField.PLACE, " Brittany ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a replaced image alone uploads the rotated photo and sends no patch`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(listOf("uploadPhoto:beach"), repository.writes)
        assertEquals("$PHOTO@${Rotation.CW_90}", repository.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", repository.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(PhotoChange(BEACH.id, ALBUM_ID)), changes)
    }

    @Test
    fun `a removed new photo leaves the server image and no change`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onPhotoRemove()

        assertNull(sut.uiState.value.photo)
        assertEquals(IMAGE_URL, sut.uiState.value.original?.imageUrl)
        assertTrue(sut.uiState.value.changes.isEmpty())
    }

    @Test
    fun `a failed patch keeps the form reachable and tells no screen`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach at noon")
        repository.editPhotoAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

        sut.uiState.value.onSave()

        assertEquals(
            AlbumSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(changes.isEmpty())
        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
        assertEquals("Beach at noon", sut.uiState.value.values.title)
    }

    @Test
    fun `a failed upload after the patch is partial and the retry sends only the upload`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach at noon")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadPhotoAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            AlbumSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_quota), isPartial = true),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)

        repository.uploadPhotoAnswer = { Result.success(BEACH) }
        sut.uiState.value.onRetrySave()

        assertEquals(listOf("editPhoto:beach", "uploadPhoto:beach", "uploadPhoto:beach"), repository.writes)
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(PhotoChange(BEACH.id, ALBUM_ID)), changes)
    }

    @Test
    fun `a failed load shows the error and the retry loads again`() {
        val sut = createSut(loaded = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)
        assertNull(sut.uiState.value.original)
        assertFalse(sut.uiState.value.canSave)

        repository.photo.clear()
        repository.photo.addLast(CompletableDeferred(Result.success(BEACH)))
        sut.uiState.value.onRetry()

        assertEquals("Beach", sut.uiState.value.values.title)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(2, repository.photoCalls.size)
    }

    @Test
    fun `the edits survive process death without a second load`() {
        val first = createSut()
        first.type(PhotoField.PLACE, "Normandy")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))

        val restored = createSut(restoredHandle())

        assertEquals("Normandy", restored.uiState.value.values.place)
        assertEquals(FormPhoto(PHOTO), restored.uiState.value.photo)
        assertEquals(IMAGE_URL, restored.uiState.value.original?.imageUrl)
        assertNull(restored.uiState.value.save)
        assertEquals(1, repository.photoCalls.size)
    }

    @Test
    fun `after process death in a save the retry sends only the calls not done`() {
        val first = createSut()
        first.type(PhotoField.TITLE, "Beach at noon")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadPhotoAnswer = { CompletableDeferred<Result<Photo>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        assertEquals(
            AlbumSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        repository.uploadPhotoAnswer = { Result.success(BEACH) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, repository.writes.count { it.startsWith("editPhoto:") })
        assertEquals(2, repository.writes.count { it.startsWith("uploadPhoto:") })
        assertTrue(restored.uiState.value.isSaved)
    }

    private companion object {
        const val ALBUM_ID = "trips"
        const val PHOTO = "content://photos/1"
        const val IMAGE_URL = "https://koillection.test/uploads/beach.jpg"
        val BEACH = photo(id = "beach", title = "Beach", albumId = ALBUM_ID).copy(
            comment = "Low tide",
            place = "Brittany",
            imageUrl = IMAGE_URL,
            thumbnailUrl = IMAGE_URL
        )
    }
}
