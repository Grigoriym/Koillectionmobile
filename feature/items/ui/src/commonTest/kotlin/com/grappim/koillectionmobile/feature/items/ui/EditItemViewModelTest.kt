package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.EditItemProgress
import com.grappim.koillectionmobile.feature.items.domain.EditItemResult
import com.grappim.koillectionmobile.feature.items.domain.EditItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.EditedField
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemEdit
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.items.domain.ItemSnapshot
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.items.domain.NewImage
import com.grappim.koillectionmobile.feature.items.domain.NewTextField
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_save_error_duplicate_label
import com.grappim.koillectionmobile.strings.generated.resources.item_error_network
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

class EditItemViewModelTest {

    private val items = FakeItemsRepository()
    private val tags = FakeTagsRepository()
    private val choiceLists = FakeChoiceLists()
    private val photoFiles = FakePhotoFiles()
    private val editUseCase = FakeEditItemUseCase()
    private val itemListChanges = ItemListChanges()
    private val itemChanges = ItemChanges()
    private val savedStateHandle = SavedStateHandle()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        tags.itemTags = Result.success(listOf(NOVEL))
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    /** Creates the ViewModel and answers the tag list at once. */
    private fun createSut(handle: SavedStateHandle = savedStateHandle): EditItemViewModel {
        tags.queue()
        val sut = EditItemViewModel(
            DUNE.id,
            items,
            tags,
            choiceLists,
            editUseCase,
            photoFiles,
            itemListChanges,
            itemChanges,
            handle
        )
        tags.tags.removeFirst().complete(Result.success(listOf(NOVEL, GERMAN)))
        tags.categories.removeFirst().complete(Result.success(emptyList<TagCategory>()))
        return sut
    }

    /** What a process death keeps: every saved-state value. */
    private fun restoredHandle() = SavedStateHandle(
        savedStateHandle.keys().associateWith {
            savedStateHandle.get<Any>(it)
        }
    )

    private val draft get(): EditItemDraft = checkNotNull(createdSut.uiState.value.draft)

    private lateinit var createdSut: EditItemViewModel

    private fun loaded(): EditItemViewModel = createSut().also { createdSut = it }

    @Test
    fun `the form is filled from the server`() {
        val sut = loaded()

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(DUNE.id), items.itemCalls)
        assertEquals(listOf(DUNE.id), items.dataCalls)
        assertEquals(listOf(DUNE.id), tags.itemTagCalls)
        assertEquals("Dune", draft.name)
        assertEquals("https://k.test/uploads/dune-large.jpg", draft.mainImage.previewUrl)
        assertNull(draft.mainImage.uri)
        assertEquals(
            listOf(
                EditRow.Text(id = 2, datumId = "author", label = "Author", value = "Frank Herbert"),
                EditRow.ReadOnly(id = 3, label = "Published", datumType = "file", value = null)
            ),
            draft.fields
        )
        assertEquals(
            listOf(
                EditPhoto(
                    id = 1,
                    datumId = "title-page",
                    previewUrl = "https://k.test/uploads/page.jpg",
                    label = "Title page"
                )
            ),
            draft.photos
        )
        assertEquals(setOf(NOVEL.id), draft.selectedTagIds)
        assertEquals(state.draft, state.original)
        assertFalse(state.hasChanges)
        assertEquals(2, state.tagGroups.single().tags.size)
    }

    @Test
    fun `an item without a main image has an empty main image row`() {
        items.item = Result.success(DUNE.copy(image = null))

        loaded()

        assertNull(draft.mainImage.previewUrl)
        assertNull(draft.mainImage.uri)
    }

    @Test
    fun `a failed load shows the error and a retry fills the form`() {
        items.item = Result.failure(KoillectionError.Network(Exception("offline")))
        val sut = loaded()

        assertNull(sut.uiState.value.draft)
        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.item_error_network), sut.uiState.value.error)

        items.item = Result.success(DUNE)
        sut.uiState.value.onRetry()

        assertEquals("Dune", draft.name)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(2, items.itemCalls.size)
    }

    @Test
    fun `text edits change the draft and not the original`() {
        val sut = loaded()
        val author = draft.fields.first().id

        sut.uiState.value.onNameChange("Dune Messiah")
        sut.uiState.value.onLabelChange(author, "Writer")
        sut.uiState.value.onValueChange(author, "F. Herbert")
        sut.uiState.value.onTagToggle(NOVEL.id)
        sut.uiState.value.onTagToggle(GERMAN.id)

        assertEquals("Dune Messiah", draft.name)
        assertEquals(EditRow.Text(author, "author", "Writer", "F. Herbert"), draft.fields.first())
        assertEquals(setOf(GERMAN.id), draft.selectedTagIds)
        assertEquals("Dune", sut.uiState.value.original?.name)
        assertTrue(sut.uiState.value.hasChanges)
    }

    @Test
    fun `a text field can be removed and a new one added after the rest`() {
        val sut = loaded()
        val author = draft.fields.first().id

        sut.uiState.value.onRemoveField(author)
        sut.uiState.value.onAddField()

        assertEquals(2, draft.fields.size)
        assertTrue(draft.fields[0] is EditRow.ReadOnly)
        val added = draft.fields[1] as EditRow.Text
        assertNull(added.datumId)
        assertEquals(4, added.id)
    }

    @Test
    fun `a read-only field is not removed or changed`() {
        val sut = loaded()
        val published = draft.fields[1].id

        sut.uiState.value.onRemoveField(published)
        sut.uiState.value.onLabelChange(published, "Year")

        assertEquals(sut.uiState.value.original?.fields, draft.fields)
    }

    @Test
    fun `a replace takes the first photo and deletes the rest`() {
        val sut = loaded()
        val main = draft.mainImage.id

        sut.uiState.value.onPhotoReplace(main, listOf("content://a", "content://b"))

        assertEquals("content://a", draft.mainImage.uri)
        assertEquals("https://k.test/uploads/dune-large.jpg", draft.mainImage.previewUrl)
        assertEquals(listOf("content://b"), photoFiles.deleted)
    }

    @Test
    fun `a second replace deletes the first replacement and resets the rotation`() {
        val sut = loaded()
        val page = draft.photos.single().id
        sut.uiState.value.onPhotoReplace(page, listOf("content://a"))
        sut.uiState.value.onPhotoRotate(page)

        sut.uiState.value.onPhotoReplace(page, listOf("content://c"))

        assertEquals("content://c", draft.photos.single().uri)
        assertEquals(Rotation.NONE, draft.photos.single().rotation)
        assertEquals(listOf("content://a"), photoFiles.deleted)
    }

    @Test
    fun `only a photo from the device rotates`() {
        val sut = loaded()
        val main = draft.mainImage.id

        sut.uiState.value.onPhotoRotate(main)
        assertEquals(Rotation.NONE, draft.mainImage.rotation)

        sut.uiState.value.onPhotoReplace(main, listOf("content://a"))
        sut.uiState.value.onPhotoRotate(main)
        assertEquals(Rotation.entries[1], draft.mainImage.rotation)
    }

    @Test
    fun `an image field can be removed but the main image cannot`() {
        val sut = loaded()
        val page = draft.photos.single().id
        sut.uiState.value.onPhotoReplace(page, listOf("content://a"))

        sut.uiState.value.onPhotoRemove(page)
        sut.uiState.value.onPhotoRemove(draft.mainImage.id)

        assertTrue(draft.photos.isEmpty())
        assertEquals(sut.uiState.value.original?.mainImage, draft.mainImage)
        assertEquals(listOf("content://a"), photoFiles.deleted)
    }

    @Test
    fun `new photos become image fields after the existing ones`() {
        val sut = loaded()

        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        sut.uiState.value.onPhotoLabelChange(draft.photos[1].id, "Back cover")

        assertEquals(listOf("title-page", null, null), draft.photos.map { it.datumId })
        assertEquals(listOf(null, "content://a", "content://b"), draft.photos.map { it.uri })
        assertEquals("Back cover", draft.photos[1].label)
    }

    @Test
    fun `a label used by a text field and an image field is a duplicate`() {
        val sut = loaded()

        sut.uiState.value.onLabelChange(draft.fields.first().id, "Title page")
        assertEquals(setOf("Title page"), sut.uiState.value.duplicateLabels)

        sut.uiState.value.onLabelChange(draft.fields.first().id, "Published")
        assertEquals(setOf("Published"), sut.uiState.value.duplicateLabels)
    }

    @Test
    fun `after a process death the edits come back and the item is not read again`() {
        val sut = loaded()
        sut.uiState.value.onNameChange("Dune Messiah")
        sut.uiState.value.onPhotoReplace(draft.mainImage.id, listOf("content://a"))

        val restored =
            createSut(restoredHandle())

        val state = restored.uiState.value
        assertEquals(1, items.itemCalls.size)
        assertFalse(state.isLoading)
        assertEquals("Dune Messiah", state.draft?.name)
        assertEquals("content://a", state.draft?.mainImage?.uri)
        assertEquals("Dune", state.original?.name)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `rows added after a restore get ids the restored rows do not use`() {
        val sut = loaded()
        sut.uiState.value.onAddField()

        val restored =
            createSut(restoredHandle())
        restored.uiState.value.onAddField()

        val ids = checkNotNull(restored.uiState.value.draft).fields.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `leaving the form deletes its photo files`() {
        val sut = loaded()
        sut.uiState.value.onPhotoReplace(draft.mainImage.id, listOf("content://a"))
        sut.uiState.value.onPhotosAdd(listOf("content://b"))
        val store = ViewModelStore()
        store.put("edit-item", sut)

        store.clear()

        assertEquals(listOf("content://a", "content://b"), photoFiles.deleted)
    }

    @Test
    fun `save is off with a blank name or a blank label`() {
        val sut = loaded()
        assertTrue(sut.uiState.value.canSave)

        sut.uiState.value.onNameChange(" ")
        assertFalse(sut.uiState.value.canSave, "a blank name")
        sut.uiState.value.onNameChange("Dune")

        sut.uiState.value.onAddField()
        assertFalse(sut.uiState.value.canSave, "a blank text label")
        sut.uiState.value.onRemoveField(draft.fields.last().id)

        sut.uiState.value.onPhotoLabelChange(draft.photos.single().id, "")
        assertFalse(sut.uiState.value.canSave, "a blank image label")

        sut.uiState.value.onSave()
        assertEquals(emptyList(), editUseCase.calls)
    }

    @Test
    fun `save sends the form against the item as loaded`() {
        val sut = loaded()
        val s = { sut.uiState.value }
        s().onNameChange(" Dune Messiah ")
        s().onValueChange(draft.fields.first().id, "F. Herbert")
        s().onAddField()
        s().onLabelChange(draft.fields.last().id, "Pages")
        s().onValueChange(draft.fields.last().id, "412")
        s().onPhotoRemove(draft.photos.single().id)
        s().onPhotoReplace(draft.mainImage.id, listOf("content://a"))
        s().onPhotoRotate(draft.mainImage.id)
        s().onPhotosAdd(listOf("content://b"))
        s().onPhotoLabelChange(draft.photos.single().id, "Spine")
        s().onTagToggle(GERMAN.id)

        s().onSave()

        val (kind, original, edit) = editUseCase.calls.single()
        assertEquals("edit", kind)
        assertEquals(ItemSnapshot(DUNE, listOf(AUTHOR, TITLE_PAGE, PUBLISHED), setOf(NOVEL.id)), original)
        assertEquals("Dune Messiah", edit.name)
        assertEquals(listOf<EditedField>(EditedField.Text("author", "Author", "F. Herbert")), edit.fields)
        assertEquals(listOf(NewTextField("Pages", "412")), edit.newTextFields)
        assertEquals(listOf("Spine" to "content://b@NONE"), edit.newImageFields.map { it.label to it.image.text() })
        assertEquals("content://a@${Rotation.entries[1]}", edit.newMainImage?.text())
        assertEquals(setOf(NOVEL.id, GERMAN.id), edit.tagIds.toSet())
    }

    @Test
    fun `a replaced field image is sent with its field and an unchanged image is not`() {
        val sut = loaded()
        sut.uiState.value.onPhotoReplace(draft.photos.single().id, listOf("content://c"))

        sut.uiState.value.onSave()

        val edit = editUseCase.calls.single().third
        val page = assertIs<EditedField.Image>(edit.fields.single { it.datumId == "title-page" })
        assertEquals("content://c@NONE", page.newImage?.text())
        assertNull(edit.newMainImage)
        assertEquals(listOf("content://c"), photoFiles.prepared)
    }

    /** Loads an item that has a `checkbox`, a `choice-list` and a `rating` field after the author. */
    private fun loadedTyped(): EditItemViewModel {
        items.data = Result.success(listOf(AUTHOR, SIGNED, FORMAT, RATING))
        return loaded()
    }

    @Test
    fun `existing typed fields load with their type value and choice list`() {
        loadedTyped()

        assertEquals(
            listOf(
                EditRow.Text(1, "author", "Author", "Frank Herbert"),
                EditRow.Text(2, "signed", "Signed", "0", "checkbox"),
                EditRow.Text(3, "format", "Format", """["Ebook"]""", "choice-list", "formats"),
                EditRow.Text(4, "rating", "Rating", "", "rating")
            ),
            draft.fields
        )
    }

    @Test
    fun `an existing checkbox switched on and a choice picked are sent as the server takes them`() {
        val sut = loadedTyped()
        sut.uiState.value.onCheckedChange(2, true)
        sut.uiState.value.onChoiceToggle(3, "Hardcover")

        sut.uiState.value.onSave()

        val edit = editUseCase.calls.single().third
        assertEquals(
            listOf<EditedField>(
                EditedField.Text("author", "Author", "Frank Herbert"),
                EditedField.Text("signed", "Signed", "1"),
                EditedField.Text("format", "Format", """["Hardcover","Ebook"]"""),
                EditedField.Text("rating", "Rating", "")
            ),
            edit.fields
        )
    }

    @Test
    fun `an existing field keeps its type and its choice list`() {
        val sut = loadedTyped()

        sut.uiState.value.onTypeChange(2, "text")
        sut.uiState.value.onChoiceListChange(3, "genres")

        assertEquals(sut.uiState.value.original, draft)
    }

    @Test
    fun `a value the server already holds does not block the save and a changed one does`() {
        val sut = loadedTyped()
        assertTrue(sut.uiState.value.canSave, "the loaded empty rating")

        sut.uiState.value.onValueChange(4, "11")
        assertFalse(sut.uiState.value.canSave)
    }

    @Test
    fun `a new typed field is sent with its type and choice list`() {
        val sut = loaded()
        val s = { sut.uiState.value }
        s().onAddField()
        val id = draft.fields.last().id
        s().onLabelChange(id, "Format")
        s().onTypeChange(id, "choice-list")
        s().onChoiceListChange(id, "formats")
        s().onChoiceToggle(id, "Paperback")

        s().onSave()

        assertEquals(
            listOf(NewTextField("Format", """["Paperback"]""", "choice-list", "formats")),
            editUseCase.calls.single().third.newTextFields
        )
    }

    @Test
    fun `the choice list of an existing field survives process death`() {
        loadedTyped()

        createSut(restoredHandle()).uiState.value.onSave()

        val original = editUseCase.calls.single().second
        assertEquals("formats", original.data.single { it.id == "format" }.choiceListId)
    }

    @Test
    fun `a read-only field is not sent`() {
        loaded().uiState.value.onSave()

        val edit = editUseCase.calls.single().third
        assertEquals(listOf("author", "title-page"), edit.fields.map { it.datumId })
    }

    @Test
    fun `a successful save deletes the photos tells the item and the collection and leaves`() {
        val sut = loaded()
        sut.uiState.value.onPhotoReplace(draft.mainImage.id, listOf("content://a"))

        val (items, collections) = signalsDuring { sut.uiState.value.onSave() }

        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf("content://a"), photoFiles.deleted)
        assertEquals(listOf(DUNE.id), items)
        assertEquals(listOf("books"), collections)
    }

    @Test
    fun `progress shows the finished and the total calls`() {
        val sut = loaded()
        val gate = CompletableDeferred<EditItemResult>()
        editUseCase.answer = { onProgress ->
            onProgress(EditItemProgress(1, 4))
            gate.await()
        }

        sut.uiState.value.onSave()

        assertEquals(SaveStatus.Sending(1, 4), sut.uiState.value.save)
        assertFalse(sut.uiState.value.isSaved)
    }

    @Test
    fun `an upload 500 names the upload quota and another 500 only the status`() {
        for (isUpload in listOf(true, false)) {
            editUseCase.calls.clear()
            editUseCase.answer = { EditItemResult.Failure(KoillectionError.Server(500, null), isUpload) }
            // A fresh saved state: the first round's started save would come back as unfinished.
            val sut = createSut(SavedStateHandle())

            sut.uiState.value.onSave()

            val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save)
            val expected = if (isUpload) {
                NativeText.Resource(RString.add_item_save_error_quota)
            } else {
                NativeText.Arguments(RString.add_item_save_error_http, listOf(500))
            }
            assertEquals(expected, failed.message, "isUpload: $isUpload")
        }
    }

    @Test
    fun `a new tag label is sent with the edit and creates nothing before the save`() {
        val sut = loaded()

        sut.uiState.value.onNewTag(" Signed ")

        assertEquals(listOf("Signed"), draft.newTagLabels)
        assertTrue(sut.uiState.value.hasChanges)
        assertTrue(editUseCase.calls.isEmpty())
        sut.uiState.value.onSave()
        val edit = editUseCase.calls.single().third
        assertEquals(listOf(NOVEL.id), edit.tagIds)
        assertEquals(listOf("Signed"), edit.newTagLabels)
    }

    @Test
    fun `a new tag label that a tag has in another case picks that tag`() {
        val sut = loaded()

        sut.uiState.value.onNewTag("GERMAN")

        assertEquals(setOf(NOVEL.id, GERMAN.id), draft.selectedTagIds)
        assertEquals(emptyList(), draft.newTagLabels)
    }

    @Test
    fun `a removed new tag label leaves no change`() {
        val sut = loaded()
        sut.uiState.value.onNewTag("Signed")

        sut.uiState.value.onNewTagRemove("Signed")

        assertFalse(sut.uiState.value.hasChanges)
    }

    @Test
    fun `a pending new tag survives process death`() {
        loaded().uiState.value.onNewTag("Signed")

        val restored = createSut(restoredHandle())

        assertEquals(listOf("Signed"), restored.uiState.value.draft?.newTagLabels)
    }

    @Test
    fun `a failed tag create is retried with the same new tag label`() {
        val sut = loaded()
        sut.uiState.value.onNewTag("Signed")
        editUseCase.answer = { onProgress ->
            onProgress(EditItemProgress(0, 1))
            EditItemResult.Failure(KoillectionError.Network(Exception("offline")))
        }
        sut.uiState.value.onSave()
        assertIs<SaveStatus.Failed>(sut.uiState.value.save)

        editUseCase.answer = { EditItemResult.Success }
        sut.uiState.value.onRetrySave()

        assertEquals(listOf("edit", "retry"), editUseCase.calls.map { it.first })
        assertEquals(listOf("Signed"), editUseCase.calls[1].third.newTagLabels)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failure after a call went through offers only the retry and the retry sends the same edit`() {
        val sut = loaded()
        sut.uiState.value.onPhotoReplace(draft.mainImage.id, listOf("content://a"))
        editUseCase.answer = { onProgress ->
            onProgress(EditItemProgress(0, 3))
            onProgress(EditItemProgress(1, 3))
            EditItemResult.Failure(KoillectionError.Network(Exception("offline")))
        }
        sut.uiState.value.onSave()

        assertTrue(assertIs<SaveStatus.Failed>(sut.uiState.value.save).isPartial)
        sut.uiState.value.onEditForm()
        assertIs<SaveStatus.Failed>(sut.uiState.value.save)

        editUseCase.answer = { EditItemResult.Success }
        sut.uiState.value.onRetrySave()

        assertEquals(listOf("edit", "retry"), editUseCase.calls.map { it.first })
        assertSame(editUseCase.calls[0].third, editUseCase.calls[1].third)
        assertEquals(listOf("content://a"), photoFiles.prepared)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failure before any call can go back to the form and the next save is a retry`() {
        val sut = loaded()
        editUseCase.answer = { EditItemResult.Failure(KoillectionError.Network(Exception("offline"))) }
        sut.uiState.value.onSave()
        assertFalse(assertIs<SaveStatus.Failed>(sut.uiState.value.save).isPartial)

        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
        sut.uiState.value.onNameChange("Dune Messiah")
        editUseCase.answer = { EditItemResult.Success }
        sut.uiState.value.onSave()

        assertEquals(listOf("edit", "retry"), editUseCase.calls.map { it.first })
        assertEquals("Dune Messiah", editUseCase.calls[1].third.name)
    }

    @Test
    fun `a duplicate label found by the use case names the label`() {
        val sut = loaded()
        editUseCase.answer = { EditItemResult.DuplicateLabel("Author") }

        sut.uiState.value.onSave()

        val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save)
        assertEquals(
            NativeText.Arguments(RString.edit_item_save_error_duplicate_label, listOf("Author")),
            failed.message
        )
        assertFalse(failed.isPartial)
    }

    @Test
    fun `a photo that cannot be read fails the save before any server call`() {
        val sut = loaded()
        sut.uiState.value.onPhotosAdd(listOf("content://broken"))
        sut.uiState.value.onPhotoLabelChange(draft.photos.last().id, "Spine")
        photoFiles.unreadable += "content://broken"

        sut.uiState.value.onSave()

        val failed = assertIs<SaveStatus.Failed>(sut.uiState.value.save)
        assertEquals(NativeText.Resource(RString.add_item_save_error_photo), failed.message)
        assertEquals(emptyList(), editUseCase.calls)
    }

    @Test
    fun `a save cut by process death comes back as a retry against the item as loaded`() {
        val sut = loaded()
        sut.uiState.value.onNameChange("Dune Messiah")
        editUseCase.answer = { CompletableDeferred<EditItemResult>().await() }
        sut.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        val failed = assertIs<SaveStatus.Failed>(restored.uiState.value.save)
        assertEquals(NativeText.Resource(RString.add_item_save_unfinished), failed.message)
        assertTrue(failed.isPartial)

        editUseCase.answer = { EditItemResult.Success }
        restored.uiState.value.onRetrySave()

        val (kind, original, edit) = editUseCase.calls.last()
        assertEquals("retry", kind)
        assertEquals(editUseCase.calls.first().second, original)
        assertEquals("Dune Messiah", edit.name)
        assertEquals(1, items.itemCalls.size)
        assertTrue(restored.uiState.value.isSaved)
    }

    /** The item ids on [itemChanges] and the collection ids on [itemListChanges] while [block] runs. */
    private fun signalsDuring(block: () -> Unit): Pair<List<String>, List<String>> {
        val changedItems = mutableListOf<String>()
        val changedCollections = mutableListOf<String>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { itemChanges.changedItems.collect { changedItems += it } }
        scope.launch { itemListChanges.changedCollections.collect { changedCollections += it } }
        block()
        scope.cancel()
        return changedItems to changedCollections
    }

    private fun NewImage.text() = bytes.decodeToString()

    /** Records each call as ("edit" or "retry", original, edit); [answer] decides the result. */
    private class FakeEditItemUseCase : EditItemUseCase {
        val calls = mutableListOf<Triple<String, ItemSnapshot, ItemEdit>>()
        var answer: suspend ((EditItemProgress) -> Unit) -> EditItemResult = { EditItemResult.Success }

        override suspend fun editItem(
            original: ItemSnapshot,
            edit: ItemEdit,
            onProgress: (EditItemProgress) -> Unit
        ): EditItemResult {
            calls += Triple("edit", original, edit)
            return answer(onProgress)
        }

        override suspend fun retry(
            original: ItemSnapshot,
            edit: ItemEdit,
            onProgress: (EditItemProgress) -> Unit
        ): EditItemResult {
            calls += Triple("retry", original, edit)
            return answer(onProgress)
        }
    }

    private class FakeItemsRepository : ItemsRepository {
        var item: Result<Item> = Result.success(DUNE)
        var data: Result<List<Datum>> = Result.success(listOf(AUTHOR, TITLE_PAGE, PUBLISHED))
        val itemCalls = mutableListOf<String>()
        val dataCalls = mutableListOf<String>()

        override suspend fun getTagItems(tagId: String): Result<List<Item>> = error("not used")

        override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

        override suspend fun getAllItems(): Result<List<Item>> = error("not used")

        override suspend fun getItem(id: String): Result<Item> {
            itemCalls += id
            return item
        }

        override suspend fun getData(itemId: String): Result<List<Datum>> {
            dataCalls += itemId
            return data
        }

        override suspend fun createItem(collectionId: String, name: String): Result<Item> = error("not used")

        override suspend fun createTextDatum(
            itemId: String,
            label: String,
            value: String,
            position: Int,
            type: String,
            choiceListId: String?
        ): Result<Datum> = error("not used")

        override suspend fun createImageDatum(itemId: String, label: String, position: Int): Result<Datum> =
            error("not used")

        override suspend fun uploadItemImage(
            itemId: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Item> = error("not used")

        override suspend fun uploadDatumImage(
            datumId: String,
            bytes: ByteArray,
            fileName: String,
            mimeType: String
        ): Result<Datum> = error("not used")

        override suspend fun renameItem(id: String, name: String): Result<Item> = error("not used")

        override suspend fun updateDatum(id: String, label: String?, value: String?): Result<Datum> = error("not used")

        override suspend fun deleteDatum(id: String): Result<Unit> = error("not used")

        override suspend fun deleteItem(id: String): Result<Unit> = error("not used")
    }

    private companion object {
        val DUNE = Item(
            id = "dune",
            name = "Dune",
            quantity = 1,
            collectionId = "books",
            image = Image(
                url = "https://k.test/uploads/dune.jpg",
                smallThumbnailUrl = null,
                largeThumbnailUrl = "https://k.test/uploads/dune-large.jpg"
            )
        )
        val AUTHOR = Datum("author", "text", "Author", "Frank Herbert", 1, null)
        val TITLE_PAGE = Datum(
            id = "title-page",
            type = "image",
            label = "Title page",
            value = null,
            position = 2,
            image = Image("https://k.test/uploads/page.jpg", null, null)
        )

        // A type the form does not edit.
        val PUBLISHED = Datum("published", "file", "Published", null, 3, null)
        val SIGNED = Datum("signed", "checkbox", "Signed", "0", 4, null)
        val FORMAT = Datum("format", "choice-list", "Format", """["Ebook"]""", 5, null, "formats")
        val RATING = Datum("rating", "rating", "Rating", null, 6, null)
        val NOVEL = Tag(id = "novel", label = "Novel", description = null, categoryId = null)
        val GERMAN = Tag(id = "german", label = "German", description = null, categoryId = null)
    }
}
