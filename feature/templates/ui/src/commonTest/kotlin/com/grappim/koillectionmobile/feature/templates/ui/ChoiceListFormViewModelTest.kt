package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceListChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_delete_error_in_use
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_validation
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

class ChoiceListFormViewModelTest {

    private val repository = FakeTemplatesRepository()
    private val choiceListChanges = ChoiceListChanges()
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

    private fun createSut(choiceListId: String? = null): ChoiceListFormViewModel {
        if (choiceListId != null && savedStateHandle.get<String>("choice_list_form") == null) {
            repository.choiceLists.addLast(CompletableDeferred())
        }
        return ChoiceListFormViewModel(choiceListId, repository, choiceListChanges, savedStateHandle)
    }

    private fun answerLists(result: Result<List<ChoiceList>> = Result.success(listOf(formats, genres))) {
        repository.choiceLists.removeFirst().complete(result)
    }

    /** The edit form of "Genres", loaded. */
    private fun editSut() = createSut(GENRES_ID).also { answerLists() }

    private val ChoiceListFormViewModel.draft get() = checkNotNull(uiState.value.draft)

    private fun ChoiceListFormViewModel.idOf(value: String) = draft.choices.single { it.value == value }.id

    private fun ChoiceListFormViewModel.addChoice(value: String) {
        uiState.value.onAddChoice()
        uiState.value.onChoiceChange(draft.choices.last().id, value)
    }

    private fun queueWrite() = CompletableDeferred<Result<ChoiceList>>().also {
        repository.choiceListWrites.addLast(it)
    }

    private fun queueDelete() = CompletableDeferred<Result<Unit>>().also { repository.choiceListDeletes.addLast(it) }

    @Test
    fun `the new form is empty and reads nothing`() {
        val sut = createSut()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.isNew)
        assertEquals(ChoiceListDraft("", emptyList()), sut.uiState.value.draft)
        assertEquals(0, repository.choiceListCalls)
        assertFalse(sut.uiState.value.canSave)
        assertFalse(sut.uiState.value.canDelete)
    }

    @Test
    fun `the edit form shows the list and its choices in order`() {
        val sut = editSut()

        assertFalse(sut.uiState.value.isNew)
        assertEquals(
            ChoiceListDraft("Genres", listOf(FormChoice(0, "Fiction"), FormChoice(1, "Poetry"))),
            sut.uiState.value.draft
        )
        // No change: nothing to save, but the list can be deleted.
        assertFalse(sut.uiState.value.canSave)
        assertTrue(sut.uiState.value.canDelete)
    }

    @Test
    fun `choices are added removed and moved`() {
        val sut = createSut()
        sut.addChoice("Fiction")
        sut.addChoice("Poetry")
        sut.addChoice("Essays")

        sut.uiState.value.onMoveUp(sut.idOf("Essays"))
        assertEquals(listOf("Fiction", "Essays", "Poetry"), sut.draft.choices.map { it.value })

        sut.uiState.value.onMoveDown(sut.idOf("Fiction"))
        assertEquals(listOf("Essays", "Fiction", "Poetry"), sut.draft.choices.map { it.value })

        // A move past either end does nothing.
        sut.uiState.value.onMoveUp(sut.idOf("Essays"))
        sut.uiState.value.onMoveDown(sut.idOf("Poetry"))
        assertEquals(listOf("Essays", "Fiction", "Poetry"), sut.draft.choices.map { it.value })

        sut.uiState.value.onRemoveChoice(sut.idOf("Fiction"))
        assertEquals(listOf("Essays", "Poetry"), sut.draft.choices.map { it.value })
    }

    @Test
    fun `the form blocks a blank name a blank choice and a duplicate choice`() {
        val sut = createSut()
        sut.addChoice("Fiction")
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onNameChange("Genres")
        assertTrue(sut.uiState.value.canSave)

        sut.addChoice(" Fiction ")
        assertEquals(setOf("Fiction"), sut.uiState.value.duplicateChoices)
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onChoiceChange(sut.draft.choices.last().id, " ")
        assertTrue(sut.uiState.value.duplicateChoices.isEmpty())
        assertFalse(sut.uiState.value.canSave)

        sut.uiState.value.onChoiceChange(sut.draft.choices.last().id, "Poetry")
        assertTrue(sut.uiState.value.canSave)
    }

    @Test
    fun `a duplicate choice sends nothing`() {
        val sut = createSut()
        sut.uiState.value.onNameChange("Genres")
        sut.addChoice("Fiction")
        sut.addChoice("Fiction")

        sut.uiState.value.onSave()

        assertTrue(repository.choiceListWriteCalls.isEmpty())
        assertFalse(sut.uiState.value.isSaving)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `create sends the trimmed form and leaves`() = runTest {
        var changes = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            choiceListChanges.changed.collect { changes++ }
        }
        val sut = createSut()
        sut.uiState.value.onNameChange(" Genres ")
        sut.addChoice(" Fiction")
        sut.addChoice("Poetry ")
        val write = queueWrite()

        sut.uiState.value.onSave()
        assertTrue(sut.uiState.value.isSaving)
        assertFalse(sut.uiState.value.canSave)
        // The form does not change while the save runs.
        sut.uiState.value.onNameChange("Other")
        assertEquals(" Genres ", sut.draft.name)

        write.complete(Result.success(genres))

        assertEquals(listOf("create:Genres:[Fiction, Poetry]"), repository.choiceListWriteCalls)
        assertTrue(sut.uiState.value.isDone)
        assertEquals(1, changes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `an edit sends the whole list in its new order`() = runTest {
        var changes = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            choiceListChanges.changed.collect { changes++ }
        }
        val sut = editSut()
        sut.uiState.value.onNameChange("Kinds")
        sut.uiState.value.onMoveUp(sut.idOf("Poetry"))
        sut.uiState.value.onRemoveChoice(sut.idOf("Fiction"))
        sut.addChoice("Essays")
        assertTrue(sut.uiState.value.canSave)
        val write = queueWrite()

        sut.uiState.value.onSave()
        write.complete(Result.success(genres))

        assertEquals(listOf("edit:$GENRES_ID:Kinds:[Poetry, Essays]"), repository.choiceListWriteCalls)
        assertTrue(sut.uiState.value.isDone)
        assertEquals(1, changes)
    }

    @Test
    fun `a failed save shows its error keeps the form and a second save goes through`() {
        val sut = editSut()
        sut.uiState.value.onNameChange("Kinds")
        val first = queueWrite()
        sut.uiState.value.onSave()
        first.complete(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))
        repository.choiceListWrites.removeFirst()

        assertEquals(NativeText.Resource(RString.collection_form_save_error_network), sut.uiState.value.actionError)
        assertFalse(sut.uiState.value.isSaving)
        assertFalse(sut.uiState.value.isDone)
        assertEquals("Kinds", sut.draft.name)

        sut.uiState.value.onActionErrorDismiss()
        assertTrue(sut.uiState.value.actionError.isEmpty())

        val second = queueWrite()
        sut.uiState.value.onSave()
        second.complete(Result.success(genres))

        assertEquals(2, repository.choiceListWriteCalls.size)
        assertTrue(sut.uiState.value.isDone)
    }

    @Test
    fun `a duplicate the server rejects shows the validation error`() {
        val sut = createSut()
        sut.uiState.value.onNameChange("Genres")
        sut.addChoice("Fiction")
        val write = queueWrite()

        sut.uiState.value.onSave()
        write.complete(Result.failure(KoillectionError.Validation("choices: unique")))

        assertEquals(NativeText.Resource(RString.collection_form_save_error_validation), sut.uiState.value.actionError)
        assertFalse(sut.uiState.value.isDone)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a delete leaves and tells the other screens`() = runTest {
        var changes = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            choiceListChanges.changed.collect { changes++ }
        }
        val sut = editSut()
        val delete = queueDelete()

        sut.uiState.value.onDelete()
        assertTrue(sut.uiState.value.isDeleting)
        assertFalse(sut.uiState.value.canDelete)
        delete.complete(Result.success(Unit))

        assertEquals(listOf(GENRES_ID), repository.choiceListDeleteCalls)
        assertTrue(sut.uiState.value.isDone)
        assertEquals(1, changes)
    }

    @Test
    fun `a delete the server refuses names a field that uses the list`() {
        val sut = editSut()
        val delete = queueDelete()

        sut.uiState.value.onDelete()
        delete.complete(Result.failure(KoillectionError.Server(500, "Internal Server Error")))

        assertEquals(
            NativeText.Arguments(RString.choice_list_delete_error_in_use, listOf(500)),
            sut.uiState.value.actionError
        )
        assertFalse(sut.uiState.value.isDone)
        assertTrue(sut.uiState.value.canDelete)
    }

    @Test
    fun `the new form cannot delete`() {
        val sut = createSut()

        sut.uiState.value.onDelete()

        assertTrue(repository.choiceListDeleteCalls.isEmpty())
    }

    @Test
    fun `a failed load shows its error and retry reads again`() {
        val sut = createSut(GENRES_ID)
        answerLists(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertEquals(NativeText.Resource(RString.templates_error_network), sut.uiState.value.error)
        assertEquals(null, sut.uiState.value.draft)

        repository.choiceLists.addLast(CompletableDeferred())
        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        answerLists()

        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("Genres", sut.draft.name)
        assertEquals(2, repository.choiceListCalls)
    }

    @Test
    fun `a list the server no longer has shows an error`() {
        val sut = createSut(GENRES_ID)
        answerLists(Result.success(listOf(formats)))

        assertEquals(NativeText.Resource(RString.choice_list_form_error_unexpected), sut.uiState.value.error)
        assertEquals(null, sut.uiState.value.draft)
    }

    @Test
    fun `the form survives process death without reading the list again`() {
        val sut = editSut()
        sut.uiState.value.onNameChange("Kinds")
        sut.addChoice("Essays")

        val restored = createSut(GENRES_ID)

        assertEquals(1, repository.choiceListCalls)
        assertEquals(sut.uiState.value.draft, restored.uiState.value.draft)
        assertTrue(restored.uiState.value.hasChanges)
        // A new row after the restore does not reuse an id.
        restored.uiState.value.onAddChoice()
        assertEquals(4, restored.draft.choices.map { it.id }.toSet().size)
    }

    private val genres = ChoiceList(GENRES_ID, "Genres", listOf("Fiction", "Poetry"))
    private val formats = ChoiceList("formats", "Formats", listOf("Ebook"))

    private companion object {
        const val GENRES_ID = "genres"
    }
}
