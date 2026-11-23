package com.grappim.koillectionmobile.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChange
import com.grappim.koillectionmobile.feature.albums.domain.AlbumChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
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

class EditAlbumViewModelTest {

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

    /** The album read answers [loaded] at once. */
    private fun createSut(
        handle: SavedStateHandle = savedStateHandle,
        loaded: Result<Album> = Result.success(JAPAN)
    ): EditAlbumViewModel {
        repository.album.clear()
        repository.album.addLast(CompletableDeferred(loaded))
        return EditAlbumViewModel(JAPAN.id, repository, photoFiles, albumChanges, handle)
    }

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    @Test
    fun `the form is filled from the server and an unchanged form sends no call`() {
        val sut = createSut()

        val state = sut.uiState.value
        assertEquals("Japan", state.title)
        assertEquals(IMAGE_URL, state.imageUrl)
        assertNull(state.photo)
        assertFalse(state.canSave)
        state.onSave()

        assertNull(sut.uiState.value.save)
        assertFalse(sut.uiState.value.isSaved)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a title changed back to the loaded one is no change`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Korea")
        sut.uiState.value.onTitleChange(" Japan ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a blank title cannot be saved`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("  ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a rename alone sends only the trimmed title then tells the screen and the parent`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("  Korea ")

        sut.uiState.value.onSave()

        assertEquals(listOf("rename:japan:Korea"), repository.writes)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(AlbumChange(JAPAN.id, PARENT_ID)), changes)
    }

    @Test
    fun `a replaced image alone uploads the rotated photo as JPEG and deletes the file`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(listOf("upload:japan"), repository.writes)
        assertEquals("$PHOTO@${Rotation.CW_90}", repository.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", repository.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a removed image alone sends only the image delete`() {
        val sut = createSut()

        sut.uiState.value.onPhotoRemove()
        assertNull(sut.uiState.value.imageUrl)
        sut.uiState.value.onSave()

        assertEquals(listOf("deleteImage:japan"), repository.writes)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `remove then a new photo uploads it and sends no image delete`() {
        val sut = createSut()
        sut.uiState.value.onPhotoRemove()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onSave()

        assertEquals(listOf("upload:japan"), repository.writes)
    }

    @Test
    fun `an album without an image offers no image delete`() {
        val sut = createSut(loaded = Result.success(JAPAN.copy(imageUrl = null)))

        sut.uiState.value.onPhotoRemove()

        assertTrue(sut.uiState.value.changes.isEmpty())
    }

    @Test
    fun `a failed first call keeps the form reachable and tells no screen`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Korea")
        repository.renameAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

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
        assertEquals("Korea", sut.uiState.value.title)
    }

    @Test
    fun `a failed upload after the rename is partial and the retry sends only the upload`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Korea")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            AlbumSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_quota), isPartial = true),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)

        repository.uploadAnswer = { Result.success(JAPAN) }
        sut.uiState.value.onRetrySave()

        assertEquals(listOf("rename:japan:Korea", "upload:japan", "upload:japan"), repository.writes)
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(AlbumChange(JAPAN.id, PARENT_ID)), changes)
    }

    @Test
    fun `the save shows its progress while the server answers`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Korea")
        sut.uiState.value.onPhotoRemove()
        val rename = CompletableDeferred<Result<Album>>()
        repository.renameAnswer = { rename.await() }

        sut.uiState.value.onSave()
        assertEquals(AlbumSaveStatus.Sending(done = 0, total = 2), sut.uiState.value.save)
        rename.complete(Result.success(JAPAN))

        assertEquals(listOf("rename:japan:Korea", "deleteImage:japan"), repository.writes)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failed load shows the error and the retry loads again`() {
        val sut = createSut(loaded = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        assertEquals(NativeText.Resource(RString.albums_error_network), sut.uiState.value.error)
        assertNull(sut.uiState.value.original)
        assertFalse(sut.uiState.value.canSave)

        repository.album.clear()
        repository.album.addLast(CompletableDeferred(Result.success(JAPAN)))
        sut.uiState.value.onRetry()

        assertEquals("Japan", sut.uiState.value.title)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(2, repository.albumCalls.size)
    }

    @Test
    fun `the edits survive process death without a second load`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Korea")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))

        val restored = createSut(restoredHandle())

        assertEquals("Korea", restored.uiState.value.title)
        assertEquals(FormPhoto(PHOTO), restored.uiState.value.photo)
        assertEquals(IMAGE_URL, restored.uiState.value.imageUrl)
        assertNull(restored.uiState.value.save)
        assertEquals(1, repository.albumCalls.size)
    }

    @Test
    fun `after process death in a save the retry sends only the calls not done`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Korea")
        first.uiState.value.onPhotoRemove()
        repository.deleteImageAnswer = { CompletableDeferred<Result<Album>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        assertEquals(
            AlbumSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        repository.deleteImageAnswer = { Result.success(JAPAN) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, repository.writes.count { it.startsWith("rename:") })
        assertEquals(2, repository.writes.count { it.startsWith("deleteImage:") })
        assertTrue(restored.uiState.value.isSaved)
    }

    private companion object {
        const val PARENT_ID = "travel"
        const val PHOTO = "content://photos/1"
        const val IMAGE_URL = "https://koillection.test/uploads/japan.jpg"
        val JAPAN = Album(id = "japan", title = "Japan", parentId = PARENT_ID, color = null, imageUrl = IMAGE_URL)
    }
}
