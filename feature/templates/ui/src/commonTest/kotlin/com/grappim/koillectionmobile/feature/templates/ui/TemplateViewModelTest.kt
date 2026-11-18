package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.template_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_http
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateViewModelTest {

    private val repository = FakeTemplatesRepository()
    private val templateChanges = TemplateChanges()
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
        repository.template.addLast(CompletableDeferred())
        repository.fields.addLast(CompletableDeferred())
        repository.choiceLists.addLast(CompletableDeferred())
    }

    private fun createSut(): TemplateViewModel {
        queue()
        return TemplateViewModel(TEMPLATE_ID, repository, templateChanges)
    }

    private fun answer(
        fieldList: Result<List<TemplateField>> = Result.success(listOf(author, read, genre)),
        choiceListList: Result<List<ChoiceList>> = Result.success(listOf(genres)),
        template: Result<Template> = Result.success(Template(TEMPLATE_ID, "Book"))
    ) {
        repository.template.removeFirst().complete(template)
        repository.fields.removeFirst().complete(fieldList)
        repository.choiceLists.removeFirst().complete(choiceListList)
    }

    @Test
    fun `the screen shows loading until all answers arrive`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        repository.template.removeFirst().complete(Result.success(Template(TEMPLATE_ID, "Book")))
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

    @Test
    fun `the name comes from the server`() {
        val sut = createSut()
        assertEquals(null, sut.uiState.value.name)

        answer(template = Result.success(Template(TEMPLATE_ID, "Novel")))

        assertEquals("Novel", sut.uiState.value.name)
        assertEquals(listOf(TEMPLATE_ID), repository.getTemplateCalls)
        assertTrue(sut.uiState.value.canChange)
    }

    @Test
    fun `a failed template read shows its error`() {
        val sut = createSut()

        answer(template = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertEquals(NativeText.Resource(RString.templates_error_network), sut.uiState.value.error)
        assertFalse(sut.uiState.value.canChange)
    }

    @Test
    fun `a change of this template reads it again and another template does not`() {
        val sut = createSut()
        answer()

        templateChanges.notifyChanged("other")
        assertEquals(1, repository.getTemplateCalls.size)

        queue()
        templateChanges.notifyChanged(TEMPLATE_ID)
        answer(fieldList = Result.success(listOf(author)), template = Result.success(Template(TEMPLATE_ID, "Novel")))

        assertEquals("Novel", sut.uiState.value.name)
        assertEquals(listOf("author"), sut.uiState.value.fields.map { it.id })
        assertEquals(2, repository.getTemplateCalls.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a delete marks the screen deleted and tells the list`() = runTest {
        val sut = createSut()
        answer()
        val changed = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            templateChanges.changedTemplates.collect { changed += it }
        }
        repository.deletes.addLast(CompletableDeferred())

        sut.uiState.value.onDelete()
        assertTrue(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.canChange)

        repository.deletes.removeFirst().complete(Result.success(Unit))

        assertFalse(sut.uiState.value.isDeleting)
        assertTrue(sut.uiState.value.isDeleted)
        assertEquals(listOf(TEMPLATE_ID), repository.deleteCalls)
        assertEquals(listOf(TEMPLATE_ID), changed)
        // The screen does not read the deleted template again.
        assertEquals(1, repository.getTemplateCalls.size)
    }

    @Test
    fun `a failed delete shows its error and keeps the screen`() {
        val sut = createSut()
        answer()
        repository.deletes.addLast(CompletableDeferred())

        sut.uiState.value.onDelete()
        repository.deletes.removeFirst().complete(Result.failure(IllegalStateException("parse")))

        assertFalse(sut.uiState.value.isDeleted)
        assertEquals(NativeText.Resource(RString.template_delete_error_unexpected), sut.uiState.value.deleteError)
        assertTrue(sut.uiState.value.canChange)

        sut.uiState.value.onDeleteErrorDismiss()

        assertTrue(sut.uiState.value.deleteError.isEmpty())
    }

    @Test
    fun `a delete before the template is loaded sends nothing`() {
        val sut = createSut()

        sut.uiState.value.onDelete()

        assertTrue(repository.deleteCalls.isEmpty())
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
