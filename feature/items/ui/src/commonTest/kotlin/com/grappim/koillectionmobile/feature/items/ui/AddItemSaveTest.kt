package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.AddItemProgress
import com.grappim.koillectionmobile.feature.items.domain.AddItemResult
import com.grappim.koillectionmobile.feature.items.domain.AddItemState
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_unfinished
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AddItemSaveTest {

    private val tags = FakeTagsRepository()
    private val useCase = FakeAddItemUseCase()
    private val photoFiles = FakePhotoFiles()
    private val itemListChanges = ItemListChanges()
    private val savedStateHandle = SavedStateHandle()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun createSut(handle: SavedStateHandle = savedStateHandle): AddItemViewModel {
        tags.queue()
        return AddItemViewModel(COLLECTION_ID, tags, useCase, photoFiles, itemListChanges, handle)
    }

    /** Name, two text rows, three photos (the first rotated), one tag. */
    private fun AddItemViewModel.fillForm() {
        val s = { uiState.value }
        s().onNameChange("Jörn Uhl")
        repeat(2) { s().onAddField() }
        s().onLabelChange(s().fields[0].id, "Author")
        s().onValueChange(s().fields[0].id, "Frenssen")
        s().onLabelChange(s().fields[1].id, "Year")
        s().onValueChange(s().fields[1].id, "1901")
        s().onPhotosAdd(listOf("content://a", "content://b", "content://c"))
        s().onPhotoRotate(s().photos[0].id)
        s().onPhotoLabelChange(s().photos[1].id, "Title page")
        s().onPhotoLabelChange(s().photos[2].id, "Spine")
        s().onTagToggle("novel")
    }

    @Test
    fun `save is off until the name is set and every label is filled and unique`() {
        val sut = createSut()
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onNameChange("Jörn Uhl")
        assertTrue(sut.uiState.value.canSave)

        sut.uiState.value.onAddField()
        assertFalse(sut.uiState.value.canSave, "a blank label")
        sut.uiState.value.onLabelChange(sut.uiState.value.fields.single().id, "Author")
        assertTrue(sut.uiState.value.canSave)

        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        assertFalse(sut.uiState.value.canSave, "an image field without a label")
        sut.uiState.value.onPhotoLabelChange(sut.uiState.value.photos[1].id, "Author")
        assertFalse(sut.uiState.value.canSave, "a duplicate label")
        sut.uiState.value.onPhotoLabelChange(sut.uiState.value.photos[1].id, "Spine")
        assertTrue(sut.uiState.value.canSave)

        sut.uiState.value.onNameChange("  ")
        assertFalse(sut.uiState.value.canSave, "a blank name")
    }

    @Test
    fun `save while it is off sends nothing`() {
        val sut = createSut()

        sut.uiState.value.onSave()

        assertTrue(useCase.calls.isEmpty())
        assertNull(sut.uiState.value.save)
    }

    @Test
    fun `save sends the form as one item with the prepared photos in order`() {
        val sut = createSut()
        sut.fillForm()

        sut.uiState.value.onSave()

        val (item, state) = useCase.calls.single()
        assertEquals(AddItemState(), state)
        assertEquals(COLLECTION_ID, item.collectionId)
        assertEquals("Jörn Uhl", item.name)
        assertEquals(listOf("Author" to "Frenssen", "Year" to "1901"), item.textFields.map { it.label to it.value })
        assertEquals("content://a@${Rotation.CW_90}", item.mainImage?.bytes?.decodeToString())
        assertEquals(listOf("Title page", "Spine"), item.imageFields.map { it.label })
        assertEquals(
            listOf("content://b@${Rotation.NONE}", "content://c@${Rotation.NONE}"),
            item.imageFields.map { it.image.bytes.decodeToString() }
        )
        assertTrue(
            (
                listOfNotNull(item.mainImage) + item.imageFields.map {
                    it.image
                }
                ).all { it.mimeType == "image/jpeg" }
        )
        assertEquals(listOf("novel"), item.tagIds)
    }

    @Test
    fun `progress shows the finished and the total calls`() {
        val gate = CompletableDeferred<AddItemResult>()
        useCase.answer = { _, _, onProgress ->
            onProgress(AddItemProgress(done = 3, total = 9, state = AddItemState(itemId = "item-1")))
            gate.await()
        }
        val sut = createSut()
        sut.fillForm()

        sut.uiState.value.onSave()

        assertEquals(SaveStatus.Sending(done = 3, total = 9), sut.uiState.value.save)
        sut.uiState.value.onSave()
        assertEquals(1, useCase.calls.size, "a second save while one runs sends nothing")
        gate.complete(AddItemResult.Success("item-1"))
    }

    @Test
    fun `a successful save deletes the photos tells the collection and gives the item id`() {
        val changed = mutableListOf<String>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            itemListChanges.changedCollections.collect { changed += it }
        }
        val sut = createSut()
        sut.fillForm()

        sut.uiState.value.onSave()

        scope.cancel()
        assertEquals(FakeAddItemUseCase.NEW_ITEM_ID, sut.uiState.value.savedItemId)
        assertEquals(listOf("content://a", "content://b", "content://c"), photoFiles.deleted)
        assertEquals(listOf(COLLECTION_ID), changed)
    }

    /** Answers the tag list of the last [createSut] with "Novel" and "German". */
    private fun answerTags() {
        tags.tags.removeFirst().complete(Result.success(listOf(NOVEL, GERMAN)))
        tags.categories.removeFirst().complete(Result.success(emptyList()))
    }

    @Test
    fun `a new tag label is kept for the save and creates nothing before it`() {
        val sut = createSut()
        answerTags()
        sut.fillForm()

        sut.uiState.value.onNewTag("  Signed ")

        assertEquals(listOf("Signed"), sut.uiState.value.newTagLabels)
        assertEquals(setOf("novel"), sut.uiState.value.selectedTagIds)
        assertTrue(useCase.calls.isEmpty())
        sut.uiState.value.onSave()
        val item = useCase.calls.single().first
        assertEquals(listOf("novel"), item.tagIds)
        assertEquals(listOf("Signed"), item.newTagLabels)
    }

    @Test
    fun `a new tag label that a tag has in another case picks that tag`() {
        val sut = createSut()
        answerTags()

        sut.uiState.value.onNewTag("german")

        assertEquals(setOf(GERMAN.id), sut.uiState.value.selectedTagIds)
        assertEquals(emptyList(), sut.uiState.value.newTagLabels)
    }

    @Test
    fun `a blank or repeated new tag label adds nothing and a pending one can be removed`() {
        val sut = createSut()
        answerTags()

        sut.uiState.value.onNewTag("Signed")
        sut.uiState.value.onNewTag("SIGNED")
        sut.uiState.value.onNewTag("  ")
        assertEquals(listOf("Signed"), sut.uiState.value.newTagLabels)
        assertTrue(sut.uiState.value.hasInput)

        sut.uiState.value.onNewTagRemove("Signed")
        assertEquals(emptyList(), sut.uiState.value.newTagLabels)
        assertFalse(sut.uiState.value.hasInput)
    }

    @Test
    fun `a pending new tag survives process death`() {
        val first = createSut()
        answerTags()
        first.uiState.value.onNewTag("Signed")

        val second = createSut()

        assertEquals(listOf("Signed"), second.uiState.value.newTagLabels)
    }

    @Test
    fun `a failed tag create is retried with the same new tag label`() {
        val atTags = AddItemState(itemId = "item-1", textFieldsDone = 2, mainImageDone = true)
        useCase.answer = { _, _, _ -> AddItemResult.Failure(KoillectionError.Network(Exception("offline")), atTags) }
        val sut = createSut()
        answerTags()
        sut.fillForm()
        sut.uiState.value.onNewTag("Signed")
        sut.uiState.value.onSave()
        assertIs<SaveStatus.Failed>(sut.uiState.value.save)

        useCase.answer = { _, _, _ -> AddItemResult.Success("item-1") }
        sut.uiState.value.onRetrySave()

        assertEquals(listOf("Signed"), useCase.calls[1].first.newTagLabels)
        assertEquals(atTags, useCase.calls[1].second)
        assertEquals("item-1", sut.uiState.value.savedItemId)
    }

    @Test
    fun `a failure keeps what exists and the retry sends the same item from there`() {
        val partial = AddItemState(itemId = "item-1", textFieldsDone = 1)
        useCase.answer = { _, _, _ -> AddItemResult.Failure(KoillectionError.Server(503, null), partial) }
        val sut = createSut()
        sut.fillForm()
        sut.uiState.value.onSave()

        val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save)
        assertTrue(failed.isPartial)
        assertEquals(NativeText.Arguments(RString.add_item_save_error_http, listOf(503)), failed.message)

        useCase.answer = { _, _, _ -> AddItemResult.Success("item-1") }
        sut.uiState.value.onRetrySave()

        assertEquals(2, useCase.calls.size)
        assertSame(useCase.calls[0].first, useCase.calls[1].first)
        assertEquals(partial, useCase.calls[1].second)
        assertEquals(3, photoFiles.prepared.size, "the retry does not prepare the photos again")
        assertEquals("item-1", sut.uiState.value.savedItemId)
    }

    @Test
    fun `an upload 500 names the upload quota`() {
        val atMainImage = AddItemState(itemId = "item-1", textFieldsDone = 2)
        val atFieldImage = atMainImage.copy(mainImageDone = true, imageFieldIds = listOf("d-1"))
        for (state in listOf(atMainImage, atFieldImage)) {
            useCase.answer = { _, _, _ -> AddItemResult.Failure(KoillectionError.Server(500, null), state) }
            // A new saved state each time: a partial failure stays in it.
            val sut = createSut(SavedStateHandle())
            sut.fillForm()

            sut.uiState.value.onSave()

            val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save, "$state")
            assertEquals(NativeText.Resource(RString.add_item_save_error_quota), failed.message, "$state")
        }
    }

    @Test
    fun `a 500 outside an upload names only the status`() {
        val atTextField = AddItemState(itemId = "item-1", textFieldsDone = 1)
        val atFieldCreate = AddItemState(itemId = "item-1", textFieldsDone = 2, mainImageDone = true)
        for (state in listOf(atTextField, atFieldCreate)) {
            useCase.answer = { _, _, _ -> AddItemResult.Failure(KoillectionError.Server(500, null), state) }
            // A new saved state each time: a partial failure stays in it.
            val sut = createSut(SavedStateHandle())
            sut.fillForm()

            sut.uiState.value.onSave()

            val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save, "$state")
            assertEquals(NativeText.Arguments(RString.add_item_save_error_http, listOf(500)), failed.message, "$state")
        }
    }

    @Test
    fun `a failure before the item exists can go back to the form`() {
        useCase.answer = { _, state, _ -> AddItemResult.Failure(KoillectionError.Network(Exception("down")), state) }
        val sut = createSut()
        sut.fillForm()
        sut.uiState.value.onSave()
        assertFalse(assertIs<SaveStatus.Failed>(sut.uiState.value.save).isPartial)

        sut.uiState.value.onEditForm()

        assertNull(sut.uiState.value.save)
        sut.uiState.value.onNameChange("Jörn Uhl, 2nd ed.")
        sut.uiState.value.onSave()
        assertEquals("Jörn Uhl, 2nd ed.", useCase.calls.last().first.name, "the next save takes the edited form")
    }

    @Test
    fun `a partial failure cannot go back to the form`() {
        useCase.answer = { _, _, _ ->
            AddItemResult.Failure(KoillectionError.Network(Exception("down")), AddItemState(itemId = "item-1"))
        }
        val sut = createSut()
        sut.fillForm()
        sut.uiState.value.onSave()

        sut.uiState.value.onEditForm()

        assertIs<SaveStatus.Failed>(sut.uiState.value.save)
    }

    @Test
    fun `a photo that cannot be read fails the save before any server call`() {
        photoFiles.unreadable += "content://b"
        val sut = createSut()
        sut.fillForm()

        sut.uiState.value.onSave()

        assertTrue(useCase.calls.isEmpty())
        val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save)
        assertEquals(NativeText.Resource(RString.add_item_save_error_photo), failed.message)
        assertFalse(failed.isPartial)
    }

    @Test
    fun `a save cut by process death comes back as a retry from the last progress`() {
        val reached = AddItemState(itemId = "item-1", textFieldsDone = 2)
        useCase.answer = { _, _, onProgress ->
            onProgress(AddItemProgress(done = 3, total = 9, state = reached))
            CompletableDeferred<AddItemResult>().await()
        }
        val first = createSut()
        first.fillForm()
        first.uiState.value.onSave()

        val restored = createSut()

        val failed = assertIs<SaveStatus.Failed>(restored.uiState.value.save)
        assertEquals(NativeText.Resource(RString.add_item_save_unfinished), failed.message)
        assertTrue(failed.isPartial)
        useCase.answer = { _, _, _ -> AddItemResult.Success("item-1") }
        restored.uiState.value.onRetrySave()
        assertEquals(reached, useCase.calls.last().second)
        assertEquals("Jörn Uhl", useCase.calls.last().first.name)
    }

    @Test
    fun `a save cut before the item exists comes back as the form`() {
        useCase.answer = { _, _, onProgress ->
            onProgress(AddItemProgress(done = 0, total = 9, state = AddItemState()))
            CompletableDeferred<AddItemResult>().await()
        }
        val first = createSut()
        first.fillForm()
        first.uiState.value.onSave()

        val restored = createSut()

        assertNull(restored.uiState.value.save)
        assertEquals("Jörn Uhl", restored.uiState.value.name)
    }

    @Test
    fun `a ViewModel restored after the save opens the item and sends nothing`() {
        val sut = createSut()
        sut.fillForm()
        sut.uiState.value.onSave()

        val restored = createSut()

        assertEquals(FakeAddItemUseCase.NEW_ITEM_ID, restored.uiState.value.savedItemId)
        assertNull(restored.uiState.value.save)
        assertEquals(1, useCase.calls.size)
    }

    @Test
    fun `removing a photo deletes its file`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))

        sut.uiState.value.onPhotoRemove(sut.uiState.value.photos[1].id)

        assertEquals(listOf("content://b"), photoFiles.deleted)
    }

    @Test
    fun `leaving the form deletes its photo files`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        val store = ViewModelStore()
        store.put("add-item", sut)

        store.clear()

        assertEquals(listOf("content://a", "content://b"), photoFiles.deleted)
    }

    private companion object {
        const val COLLECTION_ID = "books"
        val NOVEL = Tag(id = "novel", label = "Novel", description = null, categoryId = null)
        val GERMAN = Tag(id = "german", label = "German", description = null, categoryId = null)
    }
}
