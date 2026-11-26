package com.grappim.koillectionmobile.feature.loans.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.domain.LoanChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_network
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_not_found
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoanFormViewModelTest {

    private val repository = FakeLoansRepository()
    private val loanChanges = LoanChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val changes = mutableListOf<String>()
    private val savedStateHandle = SavedStateHandle()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { loanChanges.changed.collect { changes += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private fun newSut(handle: SavedStateHandle = savedStateHandle) =
        LoanFormViewModel(LoanFormRoute(ITEM_ID), repository, loanChanges, handle)

    /** The loans read answers [loaded] at once. */
    private fun editSut(
        handle: SavedStateHandle = savedStateHandle,
        loaded: Result<List<Loan>> = Result.success(listOf(OTHER, LENT))
    ): LoanFormViewModel {
        repository.loans.clear()
        repository.loans.addLast(CompletableDeferred(loaded))
        return LoanFormViewModel(LoanFormRoute(ITEM_ID, LENT.id), repository, loanChanges, handle)
    }

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    private fun queueSave(): CompletableDeferred<Result<Loan>> = CompletableDeferred<Result<Loan>>().also {
        repository.saves.addLast(it)
    }

    private fun answerSave(result: Result<Loan> = Result.success(LENT)) {
        repository.saves.removeFirst().complete(result)
    }

    @Test
    fun `a new loan starts with no name and today and reads nothing`() {
        val sut = newSut()

        val state = sut.uiState.value
        assertEquals(LoanFormValues(lentTo = "", lentAt = today()), state.values)
        assertFalse(state.isEdit)
        assertFalse(state.isLoading)
        assertFalse(state.hasInput)
        assertTrue(repository.loansCalls.isEmpty())
    }

    @Test
    fun `a new loan sends the trimmed name and the lent date`() {
        val sut = newSut()
        sut.uiState.value.onLentToChange("  Anna ")
        sut.uiState.value.onLentAtChange(LocalDate(2026, 3, 4))
        queueSave()

        sut.uiState.value.onSave()

        assertEquals(LoanSaveStatus.Sending, sut.uiState.value.save)
        answerSave()
        assertEquals(
            listOf(FakeLoansRepository.CreateCall(ITEM_ID, "Anna", LocalDate(2026, 3, 4))),
            repository.createCalls
        )
        assertTrue(repository.editCalls.isEmpty())
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(ITEM_ID), changes)
    }

    @Test
    fun `a blank name blocks the save`() {
        val sut = newSut()
        sut.uiState.value.onLentToChange("   ")

        assertFalse(sut.uiState.value.canSave)
        sut.uiState.value.onSave()

        assertNull(sut.uiState.value.save)
        assertTrue(repository.createCalls.isEmpty())
    }

    @Test
    fun `the edit form is filled from the loan with the route id`() {
        val sut = editSut()

        val state = sut.uiState.value
        assertTrue(state.isEdit)
        assertFalse(state.isLoading)
        assertEquals(LoanFormValues(lentTo = "Bob", lentAt = LocalDate(2026, 1, 10)), state.values)
        assertEquals(listOf(ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `an unchanged edit sends no call`() {
        val sut = editSut()

        assertFalse(sut.uiState.value.canSave)
        sut.uiState.value.onSave()

        assertNull(sut.uiState.value.save)
        assertTrue(repository.editCalls.isEmpty())
    }

    @Test
    fun `an edit back to the loaded values sends no call`() {
        val sut = editSut()
        sut.uiState.value.onLentToChange("Bobby")
        sut.uiState.value.onLentToChange(" Bob ")

        assertFalse(sut.uiState.value.canSave)
        assertFalse(sut.uiState.value.hasInput)
    }

    @Test
    fun `a changed name alone sends only the name`() {
        val sut = editSut()
        sut.uiState.value.onLentToChange(" Carol ")

        assertEquals(listOf(LoanChange.LentTo("Carol")), saveEdit(sut))
    }

    @Test
    fun `a changed lent date alone sends only the lent date`() {
        val sut = editSut()
        sut.uiState.value.onLentAtChange(LocalDate(2026, 1, 9))

        assertEquals(listOf(LoanChange.LentAt(LocalDate(2026, 1, 9))), saveEdit(sut))
    }

    @Test
    fun `a set returned date alone sends only the returned date`() {
        val sut = editSut()
        sut.uiState.value.onReturnedAtChange(LocalDate(2026, 2, 1))

        assertEquals(listOf(LoanChange.ReturnedAt(LocalDate(2026, 2, 1))), saveEdit(sut))
    }

    @Test
    fun `a cleared returned date sends an explicit null`() {
        val sut = editSut(loaded = Result.success(listOf(RETURNED)))
        // RETURNED is LENT (same id) with a returned date.
        sut.uiState.value.onReturnedAtChange(null)

        assertEquals(listOf(LoanChange.ReturnedAt(null)), saveEdit(sut))
    }

    @Test
    fun `a loan that is not on the server shows not found`() {
        val sut = editSut(loaded = Result.success(listOf(OTHER)))

        assertNull(sut.uiState.value.values)
        assertEquals(NativeText.Resource(RString.loan_form_not_found), sut.uiState.value.error)
    }

    @Test
    fun `a failed read shows its error and retry reads again`() {
        val sut = editSut(loaded = Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))
        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)

        repository.loans.clear()
        repository.loans.addLast(CompletableDeferred(Result.success(listOf(LENT))))
        sut.uiState.value.onRetry()

        assertEquals(LENT.toFormValues(), sut.uiState.value.values)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(ITEM_ID, ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `a failed save shows its error and retry sends it again`() {
        val sut = editSut()
        sut.uiState.value.onLentToChange("Carol")
        queueSave()
        sut.uiState.value.onSave()
        answerSave(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertEquals(
            LoanSaveStatus.Failed(NativeText.Resource(RString.error_network)),
            sut.uiState.value.save
        )
        assertFalse(sut.uiState.value.isSaved)
        assertTrue(changes.isEmpty())

        queueSave()
        sut.uiState.value.onRetrySave()
        answerSave()

        assertEquals(2, repository.editCalls.size)
        assertEquals(repository.editCalls[0], repository.editCalls[1])
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(ITEM_ID), changes)
    }

    @Test
    fun `after a failed save the form can change again`() {
        val sut = newSut()
        sut.uiState.value.onLentToChange("Anna")
        queueSave()
        sut.uiState.value.onSave()
        answerSave(Result.failure(IllegalStateException("parse")))

        sut.uiState.value.onEditForm()

        assertNull(sut.uiState.value.save)
        assertEquals("Anna", sut.uiState.value.values?.lentTo)
    }

    @Test
    fun `the edits survive process death without a second read`() {
        val sut = editSut()
        sut.uiState.value.onLentToChange("Carol")
        sut.uiState.value.onReturnedAtChange(LocalDate(2026, 2, 1))

        val restored = LoanFormViewModel(LoanFormRoute(ITEM_ID, LENT.id), repository, loanChanges, restoredHandle())

        val state = restored.uiState.value
        assertEquals(LENT.toFormValues(), state.original)
        assertEquals(LoanFormValues("Carol", LocalDate(2026, 1, 10), LocalDate(2026, 2, 1)), state.values)
        assertFalse(state.isLoading)
        assertEquals(1, repository.loansCalls.size)
    }

    @Test
    fun `a new loan survives process death`() {
        val sut = newSut()
        sut.uiState.value.onLentToChange("Anna")
        sut.uiState.value.onLentAtChange(LocalDate(2020, 5, 6))

        val restored = newSut(restoredHandle())

        assertEquals(LoanFormValues("Anna", LocalDate(2020, 5, 6)), restored.uiState.value.values)
        assertTrue(restored.uiState.value.hasInput)
    }

    /** Saves [sut] with a success answer and returns the changes the one edit call sent. */
    private fun saveEdit(sut: LoanFormViewModel): List<LoanChange> {
        assertTrue(sut.uiState.value.canSave)
        queueSave()
        sut.uiState.value.onSave()
        answerSave()
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(1, repository.editCalls.size)
        assertEquals(LENT.id, repository.editCalls.single().first)
        return repository.editCalls.single().second
    }

    private companion object {
        val LENT =
            Loan(id = "loan-1", itemId = ITEM_ID, lentTo = "Bob", lentAt = LocalDate(2026, 1, 10), returnedAt = null)
        val RETURNED = LENT.copy(returnedAt = LocalDate(2026, 1, 20))
        val OTHER =
            Loan(id = "loan-2", itemId = ITEM_ID, lentTo = "Dan", lentAt = LocalDate(2025, 6, 1), returnedAt = null)
    }
}
