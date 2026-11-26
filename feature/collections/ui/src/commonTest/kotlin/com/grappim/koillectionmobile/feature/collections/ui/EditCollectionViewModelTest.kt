package com.grappim.koillectionmobile.feature.collections.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionListChanges
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_templates_error
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

class EditCollectionViewModelTest {

    private val collections = FakeCollectionsRepository()
    private val templates = FakeTemplatesRepository()
    private val photoFiles = FakePhotoFiles()
    private val collectionListChanges = CollectionListChanges()
    private val collectionChanges = CollectionChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val parents = mutableListOf<String?>()
    private val changed = mutableListOf<String>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { collectionListChanges.changedParents.collect { parents += it } }
        scope.launch { collectionChanges.changedCollections.collect { changed += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private val savedStateHandle = SavedStateHandle()

    private fun createSut(handle: SavedStateHandle = savedStateHandle) = EditCollectionViewModel(
        NOVELS.id,
        collections,
        templates,
        photoFiles,
        collectionListChanges,
        collectionChanges,
        handle
    )

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    /** No call of any kind went to the server after the load. */
    private fun assertNoWrites() {
        assertTrue(collections.calls.none { it != "get:${NOVELS.id}" }, "calls: ${collections.calls}")
    }

    @Test
    fun `the form is filled from the server and an unchanged form sends no call`() {
        val sut = createSut()

        val state = sut.uiState.value
        assertEquals("Novels", state.title)
        assertEquals(IMAGE_URL, state.imageUrl)
        assertNull(state.photo)
        assertFalse(state.canSave)
        state.onSave()

        assertNull(sut.uiState.value.save)
        assertFalse(sut.uiState.value.isSaved)
        assertNoWrites()
    }

    @Test
    fun `a title changed back to the loaded one is no change`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Poems")
        sut.uiState.value.onTitleChange(" Novels ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertNoWrites()
    }

    @Test
    fun `a blank title cannot be saved`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("  ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertNoWrites()
    }

    @Test
    fun `a rename alone sends only the trimmed title then tells the screen and the parent list`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("  Old novels ")

        sut.uiState.value.onSave()

        assertEquals(listOf("get:novels", "rename:novels:Old novels"), collections.calls)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf<String?>(PARENT_ID), parents)
        assertEquals(listOf(NOVELS.id), changed)
    }

    @Test
    fun `a replaced image alone uploads the rotated photo as JPEG and deletes the file`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(listOf("get:novels", "upload:novels"), collections.calls)
        assertEquals("$PHOTO@${Rotation.CW_90}", collections.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", collections.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a removed image alone sends only the image delete`() {
        val sut = createSut()

        sut.uiState.value.onPhotoRemove()
        assertNull(sut.uiState.value.imageUrl)
        sut.uiState.value.onSave()

        assertEquals(listOf("get:novels", "deleteImage:novels"), collections.calls)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `remove then a new photo uploads it and sends no image delete`() {
        val sut = createSut()
        sut.uiState.value.onPhotoRemove()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onSave()

        assertEquals(listOf("get:novels", "upload:novels"), collections.calls)
    }

    @Test
    fun `remove with a new photo drops both the photo and the server image`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onPhotoRemove()

        assertNull(sut.uiState.value.photo)
        assertNull(sut.uiState.value.imageUrl)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertEquals(listOf(CollectionEditStep.DELETE_IMAGE), sut.uiState.value.changes)
    }

    @Test
    fun `a failed first call keeps the form reachable and tells no screen`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Old novels")
        collections.renameAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(parents.isEmpty())
        assertTrue(changed.isEmpty())
        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
        assertEquals("Old novels", sut.uiState.value.title)
    }

    @Test
    fun `a failed upload after the rename is partial and the retry sends only the upload`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Old novels")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        collections.uploadAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_quota),
                isPartial = true
            ),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)

        collections.uploadAnswer = { Result.success(NOVELS) }
        sut.uiState.value.onRetrySave()

        assertEquals(
            listOf("get:novels", "rename:novels:Old novels", "upload:novels", "upload:novels"),
            collections.calls
        )
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `the save shows its progress while the server answers`() {
        val sut = createSut()
        sut.uiState.value.onTitleChange("Old novels")
        sut.uiState.value.onPhotoRemove()
        val rename = CompletableDeferred<Result<Collection>>()
        collections.renameAnswer = { rename.await() }

        sut.uiState.value.onSave()
        assertEquals(FormSaveStatus.Sending(done = 0, total = 2), sut.uiState.value.save)
        rename.complete(Result.success(NOVELS))

        assertEquals(listOf("get:novels", "rename:novels:Old novels", "deleteImage:novels"), collections.calls)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failed load shows the error and the retry loads again`() {
        collections.getAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }
        val sut = createSut()
        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)
        assertNull(sut.uiState.value.original)

        collections.getAnswer = { Result.success(NOVELS) }
        sut.uiState.value.onRetry()

        assertEquals("Novels", sut.uiState.value.title)
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `the edits survive process death without a second load`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Old novels")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))

        val restored = createSut(restoredHandle())

        assertEquals("Old novels", restored.uiState.value.title)
        assertEquals(FormPhoto(PHOTO), restored.uiState.value.photo)
        assertEquals(IMAGE_URL, restored.uiState.value.imageUrl)
        assertNull(restored.uiState.value.save)
        assertEquals(1, collections.calls.count { it.startsWith("get:") })
    }

    @Test
    fun `after process death in a save the retry sends only the calls not done`() {
        val first = createSut()
        first.uiState.value.onTitleChange("Old novels")
        first.uiState.value.onPhotoRemove()
        collections.deleteImageAnswer = { CompletableDeferred<Result<Collection>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        collections.deleteImageAnswer = { Result.success(NOVELS) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, collections.calls.count { it.startsWith("rename:") })
        assertEquals(2, collections.calls.count { it.startsWith("deleteImage:") })
        assertTrue(restored.uiState.value.isSaved)
    }

    @Test
    fun `the form shows the loaded default template and the templates sorted by name ignoring case`() {
        collections.getAnswer = { Result.success(NOVELS.copy(defaultTemplateId = BOOK.id)) }

        val sut = createSut()

        assertEquals(BOOK.id, sut.uiState.value.defaultTemplateId)
        assertEquals(TemplatesUi(listOf(ALBUM, BOOK, COMIC), isLoading = false), sut.uiState.value.templates)
        assertFalse(sut.uiState.value.canSave)
    }

    @Test
    fun `a picked default template alone sends only that call then tells the screen`() {
        val sut = createSut()

        sut.uiState.value.onDefaultTemplateChange(BOOK.id)
        assertEquals(listOf(CollectionEditStep.SET_DEFAULT_TEMPLATE), sut.uiState.value.changes)
        sut.uiState.value.onSave()

        assertEquals(listOf("get:novels", "setDefault:novels:${BOOK.id}"), collections.calls)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(NOVELS.id), changed)
    }

    @Test
    fun `none clears the default template`() {
        collections.getAnswer = { Result.success(NOVELS.copy(defaultTemplateId = BOOK.id)) }
        val sut = createSut()

        sut.uiState.value.onDefaultTemplateChange(null)
        sut.uiState.value.onSave()

        assertEquals(listOf("get:novels", "setDefault:novels:null"), collections.calls)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a default template picked back to the loaded one is no change`() {
        collections.getAnswer = { Result.success(NOVELS.copy(defaultTemplateId = BOOK.id)) }
        val sut = createSut()
        sut.uiState.value.onDefaultTemplateChange(ALBUM.id)

        sut.uiState.value.onDefaultTemplateChange(BOOK.id)
        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertNoWrites()
    }

    @Test
    fun `a failed template load shows the error and the retry loads again while the rest of the form saves`() {
        templates.answer = Result.failure(KoillectionError.Network(IllegalStateException("offline")))
        val sut = createSut()
        assertEquals(
            TemplatesUi(isLoading = false, error = NativeText.Resource(RString.collection_form_templates_error)),
            sut.uiState.value.templates
        )

        templates.answer = Result.success(listOf(BOOK))
        sut.uiState.value.onRetryTemplates()
        assertEquals(TemplatesUi(listOf(BOOK), isLoading = false), sut.uiState.value.templates)

        sut.uiState.value.onTitleChange("Old novels")
        sut.uiState.value.onSave()
        assertEquals(listOf("get:novels", "rename:novels:Old novels"), collections.calls)
    }

    @Test
    fun `the picked default template survives process death and the templates load again`() {
        val first = createSut()
        first.uiState.value.onDefaultTemplateChange(COMIC.id)

        val restored = createSut(restoredHandle())

        assertEquals(COMIC.id, restored.uiState.value.defaultTemplateId)
        assertEquals(listOf(CollectionEditStep.SET_DEFAULT_TEMPLATE), restored.uiState.value.changes)
        assertEquals(2, templates.calls)
        assertEquals(1, collections.calls.count { it.startsWith("get:") })
    }

    private class Upload(val bytes: ByteArray, val mimeType: String)

    private class FakeCollectionsRepository : CollectionsRepository {
        val calls = mutableListOf<String>()
        val uploads = mutableListOf<Upload>()
        var getAnswer: suspend () -> Result<Collection> = { Result.success(NOVELS) }
        var renameAnswer: suspend () -> Result<Collection> = { Result.success(NOVELS) }
        var uploadAnswer: suspend () -> Result<Collection> = { Result.success(NOVELS) }
        var deleteImageAnswer: suspend () -> Result<Collection> = { Result.success(NOVELS) }

        override suspend fun getCollection(id: String): Result<Collection> {
            calls += "get:$id"
            return getAnswer()
        }

        override suspend fun renameCollection(id: String, title: String): Result<Collection> {
            calls += "rename:$id:$title"
            return renameAnswer()
        }

        override suspend fun uploadCollectionImage(
            id: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Collection> {
            calls += "upload:$id"
            uploads += Upload(bytes, mimeType)
            return uploadAnswer()
        }

        override suspend fun deleteCollectionImage(id: String): Result<Collection> {
            calls += "deleteImage:$id"
            return deleteImageAnswer()
        }

        override suspend fun setDefaultTemplate(id: String, templateId: String?): Result<Collection> {
            calls += "setDefault:$id:$templateId"
            return Result.success(NOVELS.copy(defaultTemplateId = templateId))
        }

        override suspend fun getCollections(): Result<List<Collection>> = error("not used")

        override suspend fun getChildren(id: String): Result<List<Collection>> = error("not used")

        override suspend fun countItems(id: String): Result<Int> = error("not used")

        override suspend fun createCollection(title: String, parentId: String?): Result<Collection> = error("not used")

        override suspend fun deleteCollection(id: String): Result<Unit> = error("not used")
    }

    /** Only the template list is read: [answer] is given at once to each read. */
    private class FakeTemplatesRepository : TemplatesRepository {
        var answer: Result<List<Template>> = Result.success(listOf(COMIC, BOOK, ALBUM))
        var calls = 0

        override suspend fun getTemplates(): Result<List<Template>> {
            calls++
            return answer
        }

        override suspend fun getTemplate(id: String): Result<Template> = error("not used")

        override suspend fun createTemplate(name: String): Result<Template> = error("not used")

        override suspend fun renameTemplate(id: String, name: String): Result<Template> = error("not used")

        override suspend fun deleteTemplate(id: String): Result<Unit> = error("not used")

        override suspend fun getFields(templateId: String): Result<List<TemplateField>> = error("not used")

        override suspend fun createField(
            templateId: String,
            name: String,
            position: Int,
            type: String,
            choiceListId: String?
        ): Result<TemplateField> = error("not used")

        override suspend fun editField(
            id: String,
            name: String?,
            position: Int?,
            type: String?,
            choiceListId: String?
        ): Result<TemplateField> = error("not used")

        override suspend fun deleteField(id: String): Result<Unit> = error("not used")

        override suspend fun getChoiceLists(): Result<List<ChoiceList>> = error("not used")

        override suspend fun createChoiceList(name: String, choices: List<String>): Result<ChoiceList> =
            error("not used")

        override suspend fun editChoiceList(id: String, name: String, choices: List<String>): Result<ChoiceList> =
            error("not used")

        override suspend fun deleteChoiceList(id: String): Result<Unit> = error("not used")
    }

    /** A photo's bytes are `"<uri>@<rotation>"`, so a test can see which photo went where. */
    private class FakePhotoFiles : PhotoFiles {
        val prepared = mutableListOf<String>()
        val deleted = mutableListOf<String>()

        override suspend fun prepare(uri: String, rotation: Rotation): ByteArray {
            prepared += uri
            return "$uri@$rotation".encodeToByteArray()
        }

        override fun delete(uris: List<String>) {
            deleted += uris
        }
    }

    private companion object {
        const val PARENT_ID = "books"
        const val PHOTO = "content://photos/1"
        const val IMAGE_URL = "https://koillection.test/uploads/novels.jpg"
        val NOVELS =
            Collection(id = "novels", title = "Novels", parentId = PARENT_ID, color = null, imageUrl = IMAGE_URL)
        val ALBUM = Template("album", "album")
        val BOOK = Template("book", "Book")
        val COMIC = Template("comic", "Comic")
    }
}
