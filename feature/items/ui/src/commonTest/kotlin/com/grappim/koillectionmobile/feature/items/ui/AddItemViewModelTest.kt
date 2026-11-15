package com.grappim.koillectionmobile.feature.items.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.ItemListChanges
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_network
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_unexpected
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddItemViewModelTest {

    private val tags = FakeTagsRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private val savedStateHandle = SavedStateHandle()

    private fun createSut(handle: SavedStateHandle = savedStateHandle) =
        AddItemViewModel("books", tags, FakeAddItemUseCase(), FakePhotoFiles(), ItemListChanges(), handle)

    private fun answer(
        tagList: Result<List<Tag>> = Result.success(listOf(NOVEL, GERMAN, FRAKTUR, LOOSE)),
        categoryList: Result<List<TagCategory>> = Result.success(listOf(LANGUAGE, GENRE))
    ) {
        tags.tags.removeFirst().complete(tagList)
        tags.categories.removeFirst().complete(categoryList)
    }

    @Test
    fun `the form starts empty and loads tags and categories`() {
        tags.queue()

        val sut = createSut()

        val state = sut.uiState.value
        assertEquals("", state.name)
        assertTrue(state.fields.isEmpty())
        assertTrue(state.isLoadingTags)
        assertEquals(1, tags.tagCalls)
        assertEquals(1, tags.categoryCalls)
    }

    @Test
    fun `tags are grouped by category sorted by label with uncategorized tags last`() {
        tags.queue()
        val sut = createSut()

        answer()

        val state = sut.uiState.value
        assertFalse(state.isLoadingTags)
        assertEquals(
            listOf(
                TagGroup(label = "Genre", tags = listOf(NOVEL)),
                TagGroup(label = "Language", tags = listOf(FRAKTUR, GERMAN)),
                TagGroup(label = null, tags = listOf(LOOSE))
            ),
            state.tagGroups
        )
    }

    @Test
    fun `a category without tags gets no group`() {
        tags.queue()
        val sut = createSut()

        answer(tagList = Result.success(listOf(NOVEL)))

        assertEquals(listOf(TagGroup(label = "Genre", tags = listOf(NOVEL))), sut.uiState.value.tagGroups)
    }

    @Test
    fun `a tag whose category is not in the list counts as uncategorized`() {
        tags.queue()
        val sut = createSut()
        val orphan = Tag(id = "orphan", label = "Orphan", description = null, categoryId = "gone")

        answer(tagList = Result.success(listOf(orphan)))

        assertEquals(listOf(TagGroup(label = null, tags = listOf(orphan))), sut.uiState.value.tagGroups)
    }

    @Test
    fun `a category without a label groups under an empty label not under no category`() {
        tags.queue()
        val sut = createSut()
        val unnamed = TagCategory(id = "unnamed", label = null, description = null, color = null)
        val tag = Tag(id = "t", label = "T", description = null, categoryId = "unnamed")

        answer(tagList = Result.success(listOf(tag)), categoryList = Result.success(listOf(unnamed)))

        assertEquals(listOf(TagGroup(label = "", tags = listOf(tag))), sut.uiState.value.tagGroups)
    }

    @Test
    fun `a failed category load shows the network error and no tags`() {
        tags.queue()
        val sut = createSut()

        answer(categoryList = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))

        val state = sut.uiState.value
        assertFalse(state.isLoadingTags)
        assertEquals(NativeText.Resource(RString.add_item_error_network), state.tagsError)
        assertTrue(state.tagGroups.isEmpty())
    }

    @Test
    fun `a failed tag load names the HTTP status`() {
        tags.queue()
        val sut = createSut()

        answer(tagList = Result.failure(KoillectionError.Http(status = 403, message = null)))

        assertEquals(NativeText.Arguments(RString.add_item_error_http, listOf(403)), sut.uiState.value.tagsError)
    }

    @Test
    fun `any other failure gets the unexpected message`() {
        tags.queue()
        val sut = createSut()

        answer(tagList = Result.failure(IllegalStateException("bad json")))

        assertEquals(NativeText.Resource(RString.add_item_error_unexpected), sut.uiState.value.tagsError)
    }

    @Test
    fun `retry after an error loads the tags again`() {
        tags.queue()
        val sut = createSut()
        answer(tagList = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        tags.queue()

        sut.uiState.value.onRetryTags()
        assertTrue(sut.uiState.value.isLoadingTags)
        assertTrue(sut.uiState.value.tagsError.isEmpty())

        answer()

        assertFalse(sut.uiState.value.isLoadingTags)
        assertTrue(sut.uiState.value.tagsError.isEmpty())
        assertEquals(3, sut.uiState.value.tagGroups.size)
        assertEquals(2, tags.tagCalls)
    }

    @Test
    fun `retry while a load runs sends no second request`() {
        tags.queue()
        val sut = createSut()

        sut.uiState.value.onRetryTags()

        assertEquals(1, tags.tagCalls)
        assertEquals(1, tags.categoryCalls)
    }

    @Test
    fun `typing the name updates it`() {
        tags.queue()
        val sut = createSut()

        sut.uiState.value.onNameChange("Dune")

        assertEquals("Dune", sut.uiState.value.name)
    }

    @Test
    fun `added rows keep their order and get distinct ids`() {
        tags.queue()
        val sut = createSut()

        sut.uiState.value.onAddField()
        sut.uiState.value.onAddField()
        sut.uiState.value.onAddField()

        val ids = sut.uiState.value.fields.map { it.id }
        assertEquals(3, ids.toSet().size)
        assertEquals(ids.sorted(), ids)
    }

    @Test
    fun `label and value changes reach only their own row`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onAddField()
        sut.uiState.value.onAddField()
        val (first, second) = sut.uiState.value.fields.map { it.id }

        sut.uiState.value.onLabelChange(second, "Author")
        sut.uiState.value.onValueChange(second, "Frank Herbert")

        assertEquals(
            listOf(FieldRow(id = first), FieldRow(id = second, label = "Author", value = "Frank Herbert")),
            sut.uiState.value.fields
        )
    }

    @Test
    fun `removing a row keeps the others in order and a new row gets a new id`() {
        tags.queue()
        val sut = createSut()
        repeat(3) { sut.uiState.value.onAddField() }
        val (first, second, third) = sut.uiState.value.fields.map { it.id }

        sut.uiState.value.onRemoveField(second)
        sut.uiState.value.onAddField()

        val ids = sut.uiState.value.fields.map { it.id }
        assertEquals(listOf(first, third), ids.take(2))
        assertTrue(ids.last() !in listOf(first, second, third))
    }

    @Test
    fun `two rows with the same label are both reported as duplicates`() {
        tags.queue()
        val sut = createSut()
        repeat(3) { sut.uiState.value.onAddField() }
        val (first, second, third) = sut.uiState.value.fields.map { it.id }

        sut.uiState.value.onLabelChange(first, "Author")
        sut.uiState.value.onLabelChange(second, "Year")
        sut.uiState.value.onLabelChange(third, "Author")

        assertEquals(setOf("Author"), sut.uiState.value.duplicateLabels)
    }

    @Test
    fun `labels are compared exactly as the server does`() {
        tags.queue()
        val sut = createSut()
        repeat(2) { sut.uiState.value.onAddField() }
        val (first, second) = sut.uiState.value.fields.map { it.id }

        sut.uiState.value.onLabelChange(first, "Author")
        sut.uiState.value.onLabelChange(second, "author")

        assertTrue(sut.uiState.value.duplicateLabels.isEmpty())
    }

    @Test
    fun `blank labels are not reported as duplicates`() {
        tags.queue()
        val sut = createSut()
        repeat(2) { sut.uiState.value.onAddField() }

        assertTrue(sut.uiState.value.duplicateLabels.isEmpty())
    }

    @Test
    fun `fixing a duplicate label clears the report`() {
        tags.queue()
        val sut = createSut()
        repeat(2) { sut.uiState.value.onAddField() }
        val (first, second) = sut.uiState.value.fields.map { it.id }
        sut.uiState.value.onLabelChange(first, "Author")
        sut.uiState.value.onLabelChange(second, "Author")

        sut.uiState.value.onRemoveField(second)

        assertTrue(sut.uiState.value.duplicateLabels.isEmpty())
    }

    @Test
    fun `a tag toggles on and off`() {
        tags.queue()
        val sut = createSut()
        answer()

        sut.uiState.value.onTagToggle(NOVEL.id)
        sut.uiState.value.onTagToggle(GERMAN.id)
        assertEquals(setOf(NOVEL.id, GERMAN.id), sut.uiState.value.selectedTagIds)

        sut.uiState.value.onTagToggle(NOVEL.id)
        assertEquals(setOf(GERMAN.id), sut.uiState.value.selectedTagIds)
    }

    /** Each call takes the next queued response. Queue one with [queue] before the call. */
    @Test
    fun `added photos keep their order and the first is the main image`() {
        tags.queue()
        val sut = createSut()

        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        sut.uiState.value.onPhotosAdd(listOf("content://c"))

        val photos = sut.uiState.value.photos
        assertEquals(listOf("content://a", "content://b", "content://c"), photos.map { it.uri })
        assertEquals(3, photos.map { it.id }.toSet().size)
        assertTrue(photos.all { it.rotation == Rotation.NONE && it.label == "" })
    }

    @Test
    fun `rotate turns only its own photo a quarter turn clockwise and wraps after 270`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        val (first, second) = sut.uiState.value.photos.map { it.id }

        val seen = (1..4).map {
            sut.uiState.value.onPhotoRotate(first)
            sut.uiState.value.photos.first().rotation
        }

        assertEquals(listOf(Rotation.CW_90, Rotation.CW_180, Rotation.CW_270, Rotation.NONE), seen)
        assertEquals(Rotation.NONE, sut.uiState.value.photos.single { it.id == second }.rotation)
    }

    @Test
    fun `removing the main image makes the next photo the main image`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b", "content://c"))

        sut.uiState.value.onPhotoRemove(sut.uiState.value.photos.first().id)

        assertEquals(listOf("content://b", "content://c"), sut.uiState.value.photos.map { it.uri })
    }

    @Test
    fun `make main moves the photo to the front and keeps the others in order`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b", "content://c"))

        sut.uiState.value.onPhotoMakeMain(sut.uiState.value.photos.last().id)

        assertEquals(listOf("content://c", "content://a", "content://b"), sut.uiState.value.photos.map { it.uri })
    }

    @Test
    fun `a photo label change reaches only its own photo`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b", "content://c"))
        val ids = sut.uiState.value.photos.map { it.id }

        sut.uiState.value.onPhotoLabelChange(ids[1], "Title page")

        assertEquals(listOf("", "Title page", ""), sut.uiState.value.photos.map { it.label })
    }

    @Test
    fun `an image field label that a text row uses is a duplicate`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onAddField()
        sut.uiState.value.onLabelChange(sut.uiState.value.fields.single().id, "Spine")
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))

        sut.uiState.value.onPhotoLabelChange(sut.uiState.value.photos[1].id, "Spine")

        assertEquals(setOf("Spine"), sut.uiState.value.duplicateLabels)
    }

    @Test
    fun `the main image label does not count as a duplicate`() {
        tags.queue()
        val sut = createSut()
        sut.uiState.value.onAddField()
        sut.uiState.value.onLabelChange(sut.uiState.value.fields.single().id, "Spine")
        sut.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        val (first, second) = sut.uiState.value.photos.map { it.id }
        sut.uiState.value.onPhotoLabelChange(first, "Spine")
        sut.uiState.value.onPhotoLabelChange(second, "Cover")

        sut.uiState.value.onPhotoMakeMain(first)

        assertTrue(sut.uiState.value.duplicateLabels.isEmpty())
    }

    @Test
    fun `a new ViewModel on the same saved state gets the form back`() {
        tags.queue()
        val first = createSut()
        first.uiState.value.onNameChange("Jörn Uhl")
        first.uiState.value.onAddField()
        first.uiState.value.onLabelChange(first.uiState.value.fields.single().id, "Author")
        first.uiState.value.onValueChange(first.uiState.value.fields.single().id, "Gustav Frenssen")
        first.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))
        first.uiState.value.onPhotoRotate(first.uiState.value.photos[0].id)
        first.uiState.value.onPhotoLabelChange(first.uiState.value.photos[1].id, "Title page")
        first.uiState.value.onTagToggle(NOVEL.id)
        val before = first.uiState.value

        val second = createSut()

        val after = second.uiState.value
        assertEquals("Jörn Uhl", after.name)
        assertEquals(before.fields, after.fields)
        assertEquals(before.photos, after.photos)
        assertEquals(Rotation.CW_90, after.photos[0].rotation)
        assertEquals(setOf(NOVEL.id), after.selectedTagIds)
        assertTrue(after.isLoadingTags)
    }

    @Test
    fun `rows added after a restore get ids the restored rows do not use`() {
        tags.queue()
        val first = createSut()
        repeat(2) { first.uiState.value.onAddField() }
        first.uiState.value.onPhotosAdd(listOf("content://a", "content://b"))

        val second = createSut()
        second.uiState.value.onAddField()
        second.uiState.value.onPhotosAdd(listOf("content://c"))

        val state = second.uiState.value
        assertEquals(3, state.fields.map { it.id }.toSet().size)
        assertEquals(3, state.photos.map { it.id }.toSet().size)
    }

    @Test
    fun `an empty form has no input so back leaves without asking`() {
        tags.queue()

        assertFalse(createSut().uiState.value.hasInput)
    }

    @Test
    fun `a blank name counts as no input`() {
        tags.queue()
        val sut = createSut()

        sut.uiState.value.onNameChange("  ")

        assertFalse(sut.uiState.value.hasInput)
    }

    @Test
    fun `a name a field row a photo or a tag each count as input`() {
        tags.queue()
        val inputs = listOf<(AddItemUiState) -> Unit>(
            { it.onNameChange("Immensee") },
            { it.onAddField() },
            { it.onPhotosAdd(listOf("content://a")) },
            { it.onTagToggle(NOVEL.id) }
        )
        inputs.forEach { input ->
            val sut = createSut(SavedStateHandle())

            input(sut.uiState.value)

            assertTrue(sut.uiState.value.hasInput)
        }
    }

    private companion object {
        val LANGUAGE = TagCategory(id = "language", label = "Language", description = null, color = "F3E5F5")
        val GENRE = TagCategory(id = "genre", label = "Genre", description = null, color = null)
        val NOVEL = Tag(id = "novel", label = "Novel", description = null, categoryId = "genre")
        val GERMAN = Tag(id = "german", label = "German", description = null, categoryId = "language")
        val FRAKTUR = Tag(id = "fraktur", label = "fraktur", description = null, categoryId = "language")
        val LOOSE = Tag(id = "loose", label = "Loose", description = null, categoryId = null)
    }
}
