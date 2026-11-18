package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.EditedTemplateField
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateProgress
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateResult
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplateEdit
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplateSnapshot
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplateFormViewModelTest {

    private val repository = FakeTemplatesRepository()
    private val useCase = FakeSaveTemplateUseCase()
    private val templateChanges = TemplateChanges()
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

    private fun createSut(templateId: String? = null): TemplateFormViewModel {
        repository.choiceLists.addLast(CompletableDeferred())
        if (templateId != null && savedStateHandle.get<String>("template_form") == null) {
            repository.template.addLast(CompletableDeferred())
            repository.fields.addLast(CompletableDeferred())
        }
        return TemplateFormViewModel(templateId, repository, useCase, templateChanges, savedStateHandle)
    }

    private fun answerChoiceLists(result: Result<List<ChoiceList>> = Result.success(listOf(genres, formats))) {
        repository.choiceLists.removeFirst().complete(result)
    }

    private fun answerTemplate() {
        repository.template.removeFirst().complete(Result.success(book))
        repository.fields.removeFirst().complete(Result.success(listOf(author, genre)))
    }

    /** An edit form of "Book", loaded. */
    private fun editSut(): TemplateFormViewModel = createSut(BOOK_ID).also {
        answerTemplate()
        answerChoiceLists()
    }

    private val TemplateFormViewModel.draft get() = checkNotNull(uiState.value.draft)

    private fun TemplateFormViewModel.idOf(name: String) = draft.fields.single { it.name == name }.id

    /** Adds a field named [name] of [type] to the form. */
    private fun TemplateFormViewModel.addField(name: String, type: String = TYPE_TEXT) {
        uiState.value.onAddField()
        val id = draft.fields.last().id
        uiState.value.onFieldNameChange(id, name)
        uiState.value.onFieldTypeChange(id, type)
    }

    private fun queueResult() = CompletableDeferred<SaveTemplateResult>().also { useCase.results.addLast(it) }

    @Test
    fun `the new form reads only the choice lists and sorts them by name`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.isNew)

        answerChoiceLists()

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(TemplateDraft("", emptyList()), sut.uiState.value.draft)
        assertEquals(listOf(formats, genres), sut.uiState.value.choiceLists)
        assertTrue(repository.getTemplateCalls.isEmpty())
        assertFalse(sut.uiState.value.canSave)
    }

    @Test
    fun `the edit form shows the template and its fields in order`() {
        val sut = editSut()

        assertFalse(sut.uiState.value.isNew)
        assertEquals("Book", sut.draft.name)
        assertEquals(
            listOf(
                FormField(0, "author", "Author", "text", null),
                FormField(1, "genre", "Genre", "choice-list", "genres")
            ),
            sut.draft.fields
        )
        assertFalse(sut.uiState.value.hasChanges)
        // An edit form without a change has nothing to save.
        assertFalse(sut.uiState.value.canSave)
    }

    @Test
    fun `a failed load shows its error and retry reads again`() {
        val sut = createSut(BOOK_ID)
        repository.template.removeFirst().complete(Result.failure(KoillectionError.Network(IllegalStateException("x"))))
        repository.fields.removeFirst().complete(Result.success(emptyList()))
        answerChoiceLists()

        assertEquals(NativeText.Resource(RString.templates_error_network), sut.uiState.value.error)
        assertNull(sut.uiState.value.draft)

        repository.choiceLists.addLast(CompletableDeferred())
        repository.template.addLast(CompletableDeferred())
        repository.fields.addLast(CompletableDeferred())
        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        answerTemplate()
        answerChoiceLists()

        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Book", sut.draft.name)
        assertEquals(listOf(BOOK_ID, BOOK_ID), repository.getTemplateCalls)
    }

    @Test
    fun `fields are added removed and moved`() {
        val sut = createSut()
        answerChoiceLists()
        sut.addField("Author")
        sut.addField("Genre")
        sut.addField("Pages", "number")

        sut.uiState.value.onMoveUp(sut.idOf("Pages"))
        assertEquals(listOf("Author", "Pages", "Genre"), sut.draft.fields.map { it.name })

        sut.uiState.value.onMoveDown(sut.idOf("Author"))
        assertEquals(listOf("Pages", "Author", "Genre"), sut.draft.fields.map { it.name })

        // A move past either end does nothing.
        sut.uiState.value.onMoveUp(sut.idOf("Pages"))
        sut.uiState.value.onMoveDown(sut.idOf("Genre"))
        assertEquals(listOf("Pages", "Author", "Genre"), sut.draft.fields.map { it.name })

        sut.uiState.value.onRemoveField(sut.idOf("Author"))
        assertEquals(listOf("Pages", "Genre"), sut.draft.fields.map { it.name })
        assertEquals("number", sut.draft.fields.first().type)
    }

    @Test
    fun `the form blocks a blank name a duplicate field name and a choice-list field without a list`() {
        val sut = createSut()
        answerChoiceLists()
        sut.addField("Author")
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onNameChange("Book")
        assertTrue(sut.uiState.value.canSave)

        sut.addField(" Author ")
        assertEquals(setOf("Author"), sut.uiState.value.duplicateNames)
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onFieldNameChange(sut.draft.fields.last().id, "Genre")
        sut.uiState.value.onFieldTypeChange(sut.idOf("Genre"), TYPE_CHOICE_LIST)
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onFieldChoiceListChange(sut.idOf("Genre"), "genres")
        assertTrue(sut.uiState.value.canSave)

        sut.uiState.value.onFieldNameChange(sut.idOf("Genre"), " ")
        assertFalse(sut.uiState.value.canSave)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `create sends the trimmed form and leaves with the new template`() = runTest {
        val changed = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            templateChanges.changedTemplates.collect { changed += it }
        }
        val sut = createSut()
        answerChoiceLists()
        sut.uiState.value.onNameChange(" Book ")
        sut.addField(" Author ")
        sut.addField("Genre", TYPE_CHOICE_LIST)
        sut.uiState.value.onFieldChoiceListChange(sut.idOf("Genre"), "genres")
        useCase.progress = listOf(SaveTemplateProgress(1, 3))
        val result = queueResult()

        sut.uiState.value.onSave()

        assertEquals(TemplateSaveStatus.Sending(1, 3), sut.uiState.value.save)
        assertEquals(
            FakeSaveTemplateUseCase.Call(
                retryId = null,
                original = null,
                edit = TemplateEdit(
                    name = "Book",
                    fields = listOf(
                        EditedTemplateField(null, "Author", "text", null),
                        EditedTemplateField(null, "Genre", "choice-list", "genres")
                    )
                )
            ),
            useCase.calls.single()
        )

        result.complete(SaveTemplateResult.Success("new"))

        assertEquals("new", sut.uiState.value.savedTemplateId)
        assertEquals(listOf("new"), changed)
    }

    @Test
    fun `an edit sends the changes against the template as loaded`() {
        val sut = editSut()
        sut.uiState.value.onNameChange("Novel")
        sut.uiState.value.onMoveUp(sut.idOf("Genre"))
        sut.uiState.value.onRemoveField(sut.idOf("Author"))
        sut.addField("Pages", "number")
        val result = queueResult()

        sut.uiState.value.onSave()
        result.complete(SaveTemplateResult.Success(BOOK_ID))

        assertEquals(
            FakeSaveTemplateUseCase.Call(
                retryId = null,
                original = TemplateSnapshot(book, listOf(author, genre)),
                edit = TemplateEdit(
                    name = "Novel",
                    fields = listOf(
                        EditedTemplateField("genre", "Genre", "choice-list", "genres"),
                        EditedTemplateField(null, "Pages", "number", null)
                    )
                )
            ),
            useCase.calls.single()
        )
        assertEquals(BOOK_ID, sut.uiState.value.savedTemplateId)
    }

    @Test
    fun `a failure before any call went through can go back to the form`() {
        val sut = editSut()
        sut.uiState.value.onNameChange("Novel")
        val result = queueResult()

        sut.uiState.value.onSave()
        result.complete(
            SaveTemplateResult.Failure(KoillectionError.Network(IllegalStateException("x")), templateId = BOOK_ID)
        )

        assertEquals(
            TemplateSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )

        sut.uiState.value.onEditForm()

        assertNull(sut.uiState.value.save)
        sut.uiState.value.onNameChange("Atlas")
        assertEquals("Atlas", sut.draft.name)
    }

    @Test
    fun `a failure part way offers only a retry and the retry reads the template again`() {
        val sut = editSut()
        sut.uiState.value.onNameChange("Novel")
        useCase.progress = listOf(SaveTemplateProgress(0, 2), SaveTemplateProgress(1, 2))
        val first = queueResult()

        sut.uiState.value.onSave()
        first.complete(SaveTemplateResult.Failure(IllegalStateException("x"), templateId = BOOK_ID))

        val failed = sut.uiState.value.save as TemplateSaveStatus.Failed
        assertTrue(failed.isPartial)
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)
        // The form cannot change while the save shows.
        sut.uiState.value.onNameChange("Atlas")
        assertEquals("Novel", sut.draft.name)

        val second = queueResult()
        sut.uiState.value.onRetrySave()
        second.complete(SaveTemplateResult.Success(BOOK_ID))

        assertEquals(BOOK_ID, useCase.calls.last().retryId)
        assertEquals(BOOK_ID, sut.uiState.value.savedTemplateId)
    }

    @Test
    fun `a new template that failed part way is retried against the created template`() {
        val sut = createSut()
        answerChoiceLists()
        sut.uiState.value.onNameChange("Book")
        sut.addField("Author")
        useCase.progress = listOf(SaveTemplateProgress(1, 2))
        val first = queueResult()

        sut.uiState.value.onSave()
        first.complete(SaveTemplateResult.Failure(IllegalStateException("x"), templateId = "new"))
        val second = queueResult()
        sut.uiState.value.onRetrySave()
        second.complete(SaveTemplateResult.Success("new"))

        assertEquals(listOf(null, "new"), useCase.calls.map { it.retryId })
        assertNull(useCase.calls.last().original)
    }

    @Test
    fun `the form survives process death without reading the template again`() {
        val first = editSut()
        first.uiState.value.onNameChange("Novel")
        first.addField("Pages", "number")

        val restored = createSut(BOOK_ID)
        assertNull(restored.uiState.value.save)
        answerChoiceLists()

        assertEquals(first.draft, restored.draft)
        assertTrue(restored.uiState.value.hasChanges)
        assertEquals(listOf(BOOK_ID), repository.getTemplateCalls)

        // A field added after the restore gets a new row id.
        restored.uiState.value.onAddField()
        assertEquals(restored.draft.fields.size, restored.draft.fields.map { it.id }.toSet().size)
    }

    @Test
    fun `a save that process death stopped offers only a retry against the template as loaded`() {
        val first = editSut()
        first.uiState.value.onNameChange("Novel")
        queueResult()
        first.uiState.value.onSave()

        val restored = createSut(BOOK_ID)
        answerChoiceLists()

        assertEquals(
            TemplateSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        val result = queueResult()
        restored.uiState.value.onRetrySave()
        result.complete(SaveTemplateResult.Success(BOOK_ID))

        assertEquals(BOOK_ID, useCase.calls.last().retryId)
        assertEquals(TemplateSnapshot(book, listOf(author, genre)), useCase.calls.last().original)
        assertEquals(BOOK_ID, restored.uiState.value.savedTemplateId)
    }

    private val book = Template(BOOK_ID, "Book")
    private val author = TemplateField("author", "Author", 1, "text", null)
    private val genre = TemplateField("genre", "Genre", 2, "choice-list", "genres")
    private val genres = ChoiceList("genres", "Genres", listOf("Fiction"))
    private val formats = ChoiceList("formats", "formats", listOf("Ebook"))

    private companion object {
        const val BOOK_ID = "book"
    }
}
