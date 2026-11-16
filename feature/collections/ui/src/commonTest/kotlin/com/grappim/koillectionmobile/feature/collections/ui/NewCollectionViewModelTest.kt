package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
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

class NewCollectionViewModelTest {

    private val collections = FakeCollectionsRepository()
    private val photoFiles = FakePhotoFiles()
    private val collectionListChanges = CollectionListChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val parents = mutableListOf<String?>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { collectionListChanges.changedParents.collect { parents += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private val savedStateHandle = SavedStateHandle()

    private fun createSut(parentId: String? = PARENT_ID, handle: SavedStateHandle = savedStateHandle) =
        NewCollectionViewModel(parentId, collections, photoFiles, collectionListChanges, handle)

    @Test
    fun `the form starts empty and a blank title cannot be saved`() {
        val sut = createSut()

        sut.uiState.value.onTitleChange("   ")
        sut.uiState.value.onSave()

        val state = sut.uiState.value
        assertNull(state.photo)
        assertFalse(state.canSave)
        assertNull(state.save)
        assertTrue(collections.createCalls.isEmpty())
    }

    @Test
    fun `a save without an image creates the child collection then opens it and tells the parent`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("  Novels ")

        sut.uiState.value.onSave()

        assertEquals(listOf<Pair<String, String?>>("Novels" to PARENT_ID), collections.createCalls)
        assertTrue(collections.uploadCalls.isEmpty())
        assertEquals(NEW_ID, sut.uiState.value.savedCollectionId)
        assertEquals(listOf<String?>(PARENT_ID), parents)
    }

    @Test
    fun `a top-level save sends no parent and tells the top-level list`() {
        val sut = createSut(parentId = null)
        sut.uiState.value.onTitleChange("Books")

        sut.uiState.value.onSave()

        assertEquals(listOf<Pair<String, String?>>("Books" to null), collections.createCalls)
        assertEquals(listOf<String?>(null), parents)
    }

    @Test
    fun `a save with an image uploads the rotated photo as JPEG to the new collection and deletes the file`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Novels")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        val upload = collections.uploadCalls.single()
        assertEquals(NEW_ID, upload.id)
        assertEquals("$PHOTO@${Rotation.CW_90}", upload.bytes.decodeToString())
        assertEquals("image/jpeg", upload.mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertEquals(NEW_ID, sut.uiState.value.savedCollectionId)
    }

    @Test
    fun `the save shows its progress while the server answers`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Novels")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        val create = CompletableDeferred<Result<Collection>>()
        val upload = CompletableDeferred<Result<Collection>>()
        collections.createAnswer = { create.await() }
        collections.uploadAnswer = { upload.await() }

        sut.uiState.value.onSave()
        assertEquals(CollectionSaveStatus.Sending(done = 0, total = 2), sut.uiState.value.save)
        create.complete(Result.success(NEW))
        assertEquals(CollectionSaveStatus.Sending(done = 1, total = 2), sut.uiState.value.save)
        upload.complete(Result.success(NEW))

        assertEquals(NEW_ID, sut.uiState.value.savedCollectionId)
    }

    @Test
    fun `a failed create keeps the form reachable and tells no list`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Novels")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        collections.createAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

        sut.uiState.value.onSave()

        assertEquals(
            CollectionSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(collections.uploadCalls.isEmpty())
        assertTrue(parents.isEmpty())
        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
        assertEquals("Novels", sut.uiState.value.title)
    }

    @Test
    fun `a failed upload is partial and the retry uploads without a second create`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Novels")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        collections.uploadAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            CollectionSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_quota),
                isPartial = true
            ),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)
        assertTrue(photoFiles.deleted.isEmpty())

        collections.uploadAnswer = { Result.success(NEW) }
        sut.uiState.value.onRetrySave()

        assertEquals(1, collections.createCalls.size)
        assertEquals(listOf(NEW_ID, NEW_ID), collections.uploadCalls.map { it.id })
        assertEquals(1, photoFiles.prepared.size)
        assertEquals(NEW_ID, sut.uiState.value.savedCollectionId)
    }

    @Test
    fun `an unreadable photo stops the save before any call`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Novels")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        photoFiles.unreadable += PHOTO

        sut.uiState.value.onSave()

        assertEquals(
            CollectionSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_photo),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(collections.createCalls.isEmpty())
    }

    @Test
    fun `a new photo replaces the old one and deletes its file and the extra picks`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

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
        assertFalse(sut.uiState.value.hasInput)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
    }

    @Test
    fun `the title and the photo survive process death`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Novels")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        first.uiState.value.onPhotoRotate()

        val restored =
            createSut(
                handle = SavedStateHandle(
                    savedStateHandle.keys().associateWith {
                        savedStateHandle.get<Any>(it)
                    }
                )
            )

        assertEquals("Novels", restored.uiState.value.title)
        assertEquals(FormPhoto(PHOTO, Rotation.CW_90), restored.uiState.value.photo)
        assertNull(restored.uiState.value.save)
    }

    @Test
    fun `after process death in a partial save the retry uploads to the created collection`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Novels")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        collections.uploadAnswer = { CompletableDeferred<Result<Collection>>().await() }
        first.uiState.value.onSave()

        val restored =
            createSut(
                handle = SavedStateHandle(
                    savedStateHandle.keys().associateWith {
                        savedStateHandle.get<Any>(it)
                    }
                )
            )
        assertEquals(
            CollectionSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        collections.uploadAnswer = { Result.success(NEW) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, collections.createCalls.size)
        assertEquals(NEW_ID, collections.uploadCalls.last().id)
        assertEquals(NEW_ID, restored.uiState.value.savedCollectionId)
    }

    private class Upload(val id: String, val bytes: ByteArray, val mimeType: String)

    private class FakeCollectionsRepository : CollectionsRepository {
        val createCalls = mutableListOf<Pair<String, String?>>()
        var createAnswer: suspend () -> Result<Collection> = { Result.success(NEW) }
        val uploadCalls = mutableListOf<Upload>()
        var uploadAnswer: suspend () -> Result<Collection> = { Result.success(NEW) }

        override suspend fun createCollection(title: String, parentId: String?): Result<Collection> {
            createCalls += title to parentId
            return createAnswer()
        }

        override suspend fun uploadCollectionImage(
            id: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Collection> {
            uploadCalls += Upload(id, bytes, mimeType)
            return uploadAnswer()
        }

        override suspend fun getCollections(): Result<List<Collection>> = error("not used")

        override suspend fun getCollection(id: String): Result<Collection> = error("not used")

        override suspend fun getChildren(id: String): Result<List<Collection>> = error("not used")

        override suspend fun countItems(id: String): Result<Int> = error("not used")

        override suspend fun renameCollection(id: String, title: String): Result<Collection> = error("not used")

        override suspend fun deleteCollectionImage(id: String): Result<Collection> = error("not used")

        override suspend fun deleteCollection(id: String): Result<Unit> = error("not used")
    }

    /** A photo's bytes are `"<uri>@<rotation>"`, so a test can see which photo went where. */
    private class FakePhotoFiles : PhotoFiles {
        val prepared = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        val unreadable = mutableSetOf<String>()

        override suspend fun prepare(uri: String, rotation: Rotation): ByteArray {
            if (uri in unreadable) error("Cannot read $uri")
            prepared += uri
            return "$uri@$rotation".encodeToByteArray()
        }

        override fun delete(uris: List<String>) {
            deleted += uris
        }
    }

    private companion object {
        const val PARENT_ID = "books"
        const val NEW_ID = "novels"
        const val PHOTO = "content://photos/1"
        const val OTHER_PHOTO = "content://photos/2"
        const val THIRD_PHOTO = "content://photos/3"
        val NEW = Collection(id = NEW_ID, title = "Novels", parentId = PARENT_ID, color = null, imageUrl = null)
    }
}
