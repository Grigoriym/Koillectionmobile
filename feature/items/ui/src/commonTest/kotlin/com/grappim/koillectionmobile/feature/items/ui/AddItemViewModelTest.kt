package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_http
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_network
import com.grappim.koillectionmobile.strings.generated.resources.add_item_error_unexpected
import kotlinx.coroutines.CompletableDeferred
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

    private fun createSut() = AddItemViewModel(tags)

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
    private class FakeTagsRepository : TagsRepository {
        val tags = ArrayDeque<CompletableDeferred<Result<List<Tag>>>>()
        val categories = ArrayDeque<CompletableDeferred<Result<List<TagCategory>>>>()
        var tagCalls = 0
        var categoryCalls = 0

        fun queue() {
            tags.addLast(CompletableDeferred())
            categories.addLast(CompletableDeferred())
        }

        override suspend fun getTags(): Result<List<Tag>> {
            tagCalls++
            return tags.first().await()
        }

        override suspend fun getTagCategories(): Result<List<TagCategory>> {
            categoryCalls++
            return categories.first().await()
        }

        override suspend fun getItemTags(itemId: String): Result<List<Tag>> = error("not used")

        override suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit> = error("not used")
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
