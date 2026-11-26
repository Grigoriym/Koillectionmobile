package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChange
import com.grappim.koillectionmobile.feature.albums.domain.PhotoChanges
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
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

class NewPhotoViewModelTest {

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

    private fun createSut(handle: SavedStateHandle = savedStateHandle) =
        NewPhotoViewModel(ALBUM_ID, repository, photoFiles, photoChanges, handle)

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    private fun NewPhotoViewModel.type(field: PhotoField, text: String) = uiState.value.onFieldChange(field, text)

    @Test
    fun `a photo without an image cannot be saved`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.type(PhotoField.PLACE, "Brittany")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(sut.uiState.value.hasInput)
        assertNull(sut.uiState.value.save)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a removed image blocks the save again`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        assertTrue(sut.uiState.value.canSave)

        sut.uiState.value.onPhotoRemove()
        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a blank title cannot be saved`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "  ")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a save with every field creates the photo then uploads the rotated image and tells the album`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, " Beach ")
        sut.type(PhotoField.COMMENT, "Low tide")
        sut.type(PhotoField.PLACE, "Brittany")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(
            listOf("createPhoto:$ALBUM_ID:Beach:Low tide:Brittany", "uploadPhoto:$CREATED_ID"),
            repository.writes
        )
        assertEquals("$PHOTO@${Rotation.CW_90}", repository.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", repository.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(PhotoChange(CREATED_ID, ALBUM_ID)), changes)
    }

    @Test
    fun `empty fields are left out of the create`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.type(PhotoField.COMMENT, "   ")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onSave()

        assertEquals("createPhoto:$ALBUM_ID:Beach:null:null", repository.writes.first())
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `the save shows its progress while the server answers`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        val create = CompletableDeferred<Result<Photo>>()
        repository.createPhotoAnswer = { _, _ -> create.await() }

        sut.uiState.value.onSave()
        assertEquals(FormSaveStatus.Sending(done = 0, total = 2), sut.uiState.value.save)
        create.complete(Result.success(photo(id = CREATED_ID, title = "Beach", albumId = ALBUM_ID)))

        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `an unreadable photo stops the save before any call and the form can be edited again`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        photoFiles.unreadable += PHOTO

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_photo), isPartial = false),
            sut.uiState.value.save
        )
        assertTrue(repository.writes.isEmpty())
        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
    }

    @Test
    fun `a failed create keeps the form reachable and the retry creates again`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.createPhotoAnswer = { _, _ ->
            Result.failure(KoillectionError.Network(IllegalStateException("offline")))
        }

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(changes.isEmpty())

        repository.createPhotoAnswer = { _, title -> Result.success(photo(id = CREATED_ID, title = title)) }
        sut.uiState.value.onRetrySave()

        assertEquals(2, repository.writes.count { it.startsWith("createPhoto:") })
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failed upload is partial and the retry uploads without a second create`() {
        val sut = createSut()
        sut.type(PhotoField.TITLE, "Beach")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadPhotoAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_quota), isPartial = true),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)
        assertTrue(changes.isEmpty())

        repository.uploadPhotoAnswer = { Result.success(photo(id = CREATED_ID, title = "Beach")) }
        sut.uiState.value.onRetrySave()

        assertEquals(
            listOf("createPhoto:$ALBUM_ID:Beach:null:null", "uploadPhoto:$CREATED_ID", "uploadPhoto:$CREATED_ID"),
            repository.writes
        )
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(PhotoChange(CREATED_ID, ALBUM_ID)), changes)
    }

    @Test
    fun `the form survives process death`() {
        val first = createSut()
        first.type(PhotoField.TITLE, "Beach")
        first.type(PhotoField.PLACE, "Brittany")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))

        val restored = createSut(restoredHandle())

        assertEquals(PhotoFormValues(title = "Beach", place = "Brittany"), restored.uiState.value.values)
        assertEquals(FormPhoto(PHOTO), restored.uiState.value.photo)
        assertNull(restored.uiState.value.save)
    }

    @Test
    fun `after process death in the upload the retry uploads to the created photo`() {
        val first = createSut()
        first.type(PhotoField.TITLE, "Beach")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadPhotoAnswer = { CompletableDeferred<Result<Photo>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        repository.uploadPhotoAnswer = { Result.success(photo(id = CREATED_ID, title = "Beach")) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, repository.writes.count { it.startsWith("createPhoto:") })
        assertEquals(2, repository.writes.count { it == "uploadPhoto:$CREATED_ID" })
        assertTrue(restored.uiState.value.isSaved)
    }

    private companion object {
        const val ALBUM_ID = "trips"
        const val PHOTO = "content://photos/1"
    }
}
