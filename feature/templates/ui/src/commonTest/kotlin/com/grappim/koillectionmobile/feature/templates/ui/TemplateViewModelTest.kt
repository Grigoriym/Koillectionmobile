package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.template_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_http
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateViewModelTest {

    private val repository = FakeTemplatesRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun queue() {
        repository.fields.addLast(CompletableDeferred())
        repository.choiceLists.addLast(CompletableDeferred())
    }

    private fun createSut(): TemplateViewModel {
        queue()
        return TemplateViewModel(TEMPLATE_ID, repository)
    }

    private fun answer(
        fieldList: Result<List<TemplateField>> = Result.success(listOf(author, read, genre)),
        choiceListList: Result<List<ChoiceList>> = Result.success(listOf(genres))
    ) {
        repository.fields.removeFirst().complete(fieldList)
        repository.choiceLists.removeFirst().complete(choiceListList)
    }

    @Test
    fun `the screen shows loading until both answers arrive`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        repository.fields.removeFirst().complete(Result.success(listOf(author)))
        assertTrue(sut.uiState.value.isLoading)

        repository.choiceLists.removeFirst().complete(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(TEMPLATE_ID), repository.fieldCalls)
        assertEquals(1, repository.choiceListCalls)
    }

    @Test
    fun `rows keep the position order of the repository with their type and choice list`() {
        val sut = createSut()

        answer()

        assertEquals(
            listOf(
                TemplateFieldRow(id = "author", name = "Author", type = "text", choiceListName = null),
                TemplateFieldRow(id = "read", name = "Read", type = "checkbox", choiceListName = null),
                TemplateFieldRow(id = "genre", name = "Genre", type = "choice-list", choiceListName = "Genres")
            ),
            sut.uiState.value.fields
        )
    }

    @Test
    fun `a choice list the server does not have leaves the name empty`() {
        val sut = createSut()

        answer(choiceListList = Result.success(emptyList()))

        assertEquals(null, sut.uiState.value.fields.single { it.id == "genre" }.choiceListName)
    }

    @Test
    fun `no fields is empty with no error`() {
        val sut = createSut()

        answer(fieldList = Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.fields.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a failed field load shows the network error and no rows`() {
        val sut = createSut()

        answer(fieldList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.templates_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.fields.isEmpty())
    }

    @Test
    fun `a failed choice list load shows its error and no rows`() {
        val sut = createSut()

        answer(choiceListList = Result.failure(KoillectionError.Server(status = 503, message = null)))

        assertEquals(NativeText.Arguments(RString.templates_error_http, listOf(503)), sut.uiState.value.error)
        assertTrue(sut.uiState.value.fields.isEmpty())
    }

    @Test
    fun `an unexpected failure names the fields of the template`() {
        val sut = createSut()

        answer(fieldList = Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.template_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(fieldList = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(fieldList = Result.success(listOf(author)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf("author"), sut.uiState.value.fields.map { it.id })
        assertEquals(listOf(TEMPLATE_ID, TEMPLATE_ID), repository.fieldCalls)
        assertEquals(2, repository.choiceListCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.fieldCalls.size)
        assertEquals(1, repository.choiceListCalls)
    }

    private val author = TemplateField(id = "author", name = "Author", position = 1, type = "text", choiceListId = null)
    private val read = TemplateField(id = "read", name = "Read", position = 2, type = "checkbox", choiceListId = null)
    private val genre =
        TemplateField(id = "genre", name = "Genre", position = 3, type = "choice-list", choiceListId = "genres")
    private val genres = ChoiceList(id = "genres", name = "Genres", choices = listOf("Fiction", "Poetry"))

    private companion object {
        const val TEMPLATE_ID = "book"
    }
}
