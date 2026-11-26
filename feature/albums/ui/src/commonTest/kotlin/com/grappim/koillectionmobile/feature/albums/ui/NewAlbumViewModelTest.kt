package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChange
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
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

class NewAlbumViewModelTest {

    private val repository = FakeAlbumsRepository()
    private val photoFiles = FakePhotoFiles()
    private val albumChanges = AlbumChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val changes = mutableListOf<AlbumChange>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { albumChanges.changed.collect { changes += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private val savedStateHandle = SavedStateHandle()

    private fun createSut(parentId: String? = PARENT_ID, handle: SavedStateHandle = savedStateHandle) =
        NewAlbumViewModel(parentId, repository, photoFiles, albumChanges, handle)

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    @Test
    fun `the form starts empty and a blank title cannot be saved`() {
        val sut = createSut()

        sut.uiState.value.onTitleChange("   ")
        sut.uiState.value.onSave()

        val state = sut.uiState.value
        assertNull(state.photo)
        assertFalse(state.canSave)
        assertNull(state.save)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a save without an image creates the child with the trimmed title and tells the parent`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("  Japan ")

        sut.uiState.value.onSave()

        assertEquals(listOf("create:Japan:$PARENT_ID"), repository.writes)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(AlbumChange(CREATED_ID, PARENT_ID)), changes)
    }

    @Test
    fun `a top-level save sends no parent and tells the top-level list`() {
        val sut = createSut(parentId = null)
        sut.uiState.value.onTitleChange("Travel")

        sut.uiState.value.onSave()

        assertEquals(listOf("create:Travel:null"), repository.writes)
        assertEquals(listOf(AlbumChange(CREATED_ID, null)), changes)
    }

    @Test
    fun `a save with an image uploads the rotated photo as JPEG to the new album and deletes the file`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Japan")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(listOf("create:Japan:$PARENT_ID", "upload:$CREATED_ID"), repository.writes)
        assertEquals("$PHOTO@${Rotation.CW_90}", repository.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", repository.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `the save shows its progress while the server answers`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Japan")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        val upload = CompletableDeferred<Result<Album>>()
        repository.uploadAnswer = { upload.await() }

        sut.uiState.value.onSave()
        assertEquals(FormSaveStatus.Sending(done = 1, total = 2), sut.uiState.value.save)
        upload.complete(Result.success(album(CREATED_ID, "Japan")))

        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failed create keeps the form reachable and tells no screen`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Japan")
        repository.createAnswer = { _, _ -> Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(changes.isEmpty())
        assertFalse(sut.uiState.value.isSaved)
        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
        assertEquals("Japan", sut.uiState.value.title)
    }

    @Test
    fun `a failed upload is partial and the retry uploads without a second create`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Japan")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_quota), isPartial = true),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)

        repository.uploadAnswer = { Result.success(album(CREATED_ID, "Japan")) }
        sut.uiState.value.onRetrySave()

        assertEquals(
            listOf("create:Japan:$PARENT_ID", "upload:$CREATED_ID", "upload:$CREATED_ID"),
            repository.writes
        )
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(AlbumChange(CREATED_ID, PARENT_ID)), changes)
    }

    @Test
    fun `an unreadable photo stops the save before any call`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Japan")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        photoFiles.unreadable += PHOTO

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_photo), isPartial = false),
            sut.uiState.value.save
        )
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a new photo replaces the old one and deletes its file and the extra picks`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onPhotosAdd(listOf(OTHER_PHOTO, THIRD_PHOTO))

        assertEquals(FormPhoto(OTHER_PHOTO), sut.uiState.value.photo)
        assertEquals(listOf(PHOTO, THIRD_PHOTO), photoFiles.deleted)
    }

    @Test
    fun `remove drops the photo and deletes its file`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onPhotoRemove()

        assertNull(sut.uiState.value.photo)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
    }

    @Test
    fun `the title and the photo survive process death`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Japan")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        first.uiState.value.onPhotoRotate()

        val restored = createSut(handle = restoredHandle())

        assertEquals("Japan", restored.uiState.value.title)
        assertEquals(FormPhoto(PHOTO, Rotation.CW_90), restored.uiState.value.photo)
        assertNull(restored.uiState.value.save)
    }

    @Test
    fun `after process death in a partial save the retry uploads to the created album`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Japan")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadAnswer = { CompletableDeferred<Result<Album>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(handle = restoredHandle())
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        repository.uploadAnswer = { Result.success(album(CREATED_ID, "Japan")) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, repository.writes.count { it.startsWith("create:") })
        assertEquals(2, repository.writes.count { it.startsWith("upload:") })
        assertTrue(restored.uiState.value.isSaved)
    }

    private companion object {
        const val PARENT_ID = "travel"
        const val PHOTO = "content://photos/1"
        const val OTHER_PHOTO = "content://photos/2"
        const val THIRD_PHOTO = "content://photos/3"
    }
}
