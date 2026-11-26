package com.grappim.koillectionmobile.feature.tags.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_http
import com.grappim.koillectionmobile.strings.generated.resources.error_network
import com.grappim.koillectionmobile.strings.generated.resources.tags_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagListViewModelTest {

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

    private fun answer(
        tagList: Result<List<Tag>> = Result.success(listOf(german, novel, loose, fraktur, apple)),
        categoryList: Result<List<TagCategory>> = Result.success(listOf(language, genre))
    ) {
        tags.tags.removeFirst().complete(tagList)
        tags.categories.removeFirst().complete(categoryList)
    }

    private fun createSut(): TagListViewModel {
        tags.queue()
        return TagListViewModel(tags)
    }

    @Test
    fun `the screen shows loading until both answers arrive`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        tags.tags.removeFirst().complete(Result.success(listOf(novel)))
        assertTrue(sut.uiState.value.isLoading)

        tags.categories.removeFirst().complete(Result.success(listOf(genre)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(1, tags.tagCalls)
        assertEquals(1, tags.categoryCalls)
    }

    @Test
    fun `one section per category sorted by label ignoring case then the tags without a category`() {
        val sut = createSut()

        answer()

        assertEquals(
            listOf(
                TagGroup(label = "genre", color = null, tags = listOf(novel)),
                TagGroup(label = "Language", color = "F3E5F5", tags = listOf(fraktur, german)),
                TagGroup(label = null, color = null, tags = listOf(apple, loose))
            ),
            sut.uiState.value.groups
        )
    }

    @Test
    fun `tags inside a section are sorted by label ignoring case`() {
        val sut = createSut()
        val upper = Tag(id = "b", label = "B", description = null, categoryId = null)
        val lower = Tag(id = "a", label = "a", description = null, categoryId = null)
        val upperC = Tag(id = "c", label = "C", description = null, categoryId = null)

        answer(tagList = Result.success(listOf(upperC, upper, lower)))

        assertEquals(listOf(lower, upper, upperC), sut.uiState.value.groups.single().tags)
    }

    @Test
    fun `no tags is empty with no error`() {
        val sut = createSut()

        answer(tagList = Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.groups.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a failed tag load shows the network error and no sections`() {
        val sut = createSut()

        answer(tagList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.groups.isEmpty())
    }

    @Test
    fun `a failed category load shows its error and no sections`() {
        val sut = createSut()

        answer(categoryList = Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.error_http, listOf(500)), sut.uiState.value.error)
        assertTrue(sut.uiState.value.groups.isEmpty())
    }

    @Test
    fun `an unexpected failure names the tags`() {
        val sut = createSut()

        answer(tagList = Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.tags_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(tagList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        tags.queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(tagList = Result.success(listOf(novel)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(TagGroup(label = "genre", color = null, tags = listOf(novel))), sut.uiState.value.groups)
        assertEquals(2, tags.tagCalls)
        assertEquals(2, tags.categoryCalls)
    }

    @Test
    fun `a load with several sections starts with all of them closed`() {
        val sut = createSut()

        answer()

        assertEquals(emptySet(), sut.uiState.value.expandedGroups)
    }

    @Test
    fun `a load with one section starts with it open`() {
        val sut = createSut()

        answer(tagList = Result.success(listOf(novel)))

        assertEquals(setOf(0), sut.uiState.value.expandedGroups)
    }

    @Test
    fun `toggling a section opens it and toggling again closes it`() {
        val sut = createSut()
        answer()

        sut.uiState.value.onToggleGroup(1)
        sut.uiState.value.onToggleGroup(2)
        assertEquals(setOf(1, 2), sut.uiState.value.expandedGroups)

        sut.uiState.value.onToggleGroup(1)
        assertEquals(setOf(2), sut.uiState.value.expandedGroups)
    }

    @Test
    fun `a reload after a failure starts closed again`() {
        val sut = createSut()
        answer()
        sut.uiState.value.onToggleGroup(0)

        tags.queue()
        sut.uiState.value.onRetry()
        assertEquals(setOf(0), sut.uiState.value.expandedGroups)
        answer(tagList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))
        assertEquals(emptySet(), sut.uiState.value.expandedGroups)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, tags.tagCalls)
        assertEquals(1, tags.categoryCalls)
    }

    private val language = TagCategory(id = "language", label = "Language", description = null, color = "F3E5F5")
    private val genre = TagCategory(id = "genre", label = "genre", description = null, color = null)
    private val german = Tag(id = "german", label = "German", description = null, categoryId = "language")
    private val fraktur = Tag(id = "fraktur", label = "fraktur", description = null, categoryId = "language")
    private val novel = Tag(id = "novel", label = "Novel", description = null, categoryId = "genre")
    private val loose = Tag(id = "loose", label = "Loose", description = null, categoryId = null)
    private val apple = Tag(id = "apple", label = "apple", description = null, categoryId = null)

    /** Each call takes the first queued response. Queue one with [queue] before the call. */
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

        override suspend fun createTag(label: String): Result<Tag> = error("not used")

        override suspend fun getItemTags(itemId: String): Result<List<Tag>> = error("not used")

        override suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit> = error("not used")
    }
}
