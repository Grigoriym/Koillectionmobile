package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.domain.LoanChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.loans_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.loans_error_http
import com.grappim.koillectionmobile.strings.generated.resources.loans_error_network
import com.grappim.koillectionmobile.strings.generated.resources.loans_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoansViewModelTest {

    private val repository = FakeLoansRepository()
    private val loanChanges = LoanChanges()
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
        repository.loans.addLast(CompletableDeferred())
    }

    private fun createSut(): LoansViewModel {
        queue()
        return LoansViewModel(ITEM_ID, repository, loanChanges)
    }

    private fun answer(result: Result<List<Loan>>) {
        repository.loans.removeFirst().complete(result)
    }

    @Test
    fun `the section shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer(Result.success(listOf(open2024)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `loans not returned come first and each group is newest lent first`() {
        val sut = createSut()

        answer(Result.success(listOf(returned2023, open2024, returned2025, open2026)))

        assertEquals(listOf(open2026, open2024, returned2025, returned2023), sut.uiState.value.loans)
    }

    @Test
    fun `no loans is empty with no error`() {
        val sut = createSut()

        answer(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.loans.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure shows its error and no loans`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.loans_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.loans.isEmpty())
    }

    @Test
    fun `an http failure shows its status`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.loans_error_http, listOf(500)), sut.uiState.value.error)
    }

    @Test
    fun `a proxy rejection shows the proxy message`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.ProxyRejection(status = 403)))

        assertEquals(NativeText.Arguments(RString.error_proxy_rejected, listOf(403)), sut.uiState.value.error)
    }

    @Test
    fun `an unexpected failure names the loans`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.loans_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(Result.success(listOf(open2024)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(open2024), sut.uiState.value.loans)
        assertEquals(listOf(ITEM_ID, ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.loansCalls.size)
    }

    @Test
    fun `a saved loan of this item reads the loans again`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024)))

        queue()
        loanChanges.notifyChanged(ITEM_ID)

        assertTrue(sut.uiState.value.isLoading)
        answer(Result.success(listOf(open2024, open2026)))

        assertEquals(listOf(open2026, open2024), sut.uiState.value.loans)
        assertEquals(listOf(ITEM_ID, ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `a saved loan of another item reads nothing`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024)))

        loanChanges.notifyChanged("item-2")

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(1, repository.loansCalls.size)
    }

    @Test
    fun `mark returned sends today as the returned date and reads again`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024)))

        repository.saves.addLast(CompletableDeferred())
        sut.uiState.value.onMarkReturned(open2024.id)

        assertEquals(open2024.id, sut.uiState.value.busyLoanId)
        assertEquals(listOf(open2024.id to listOf<LoanChange>(LoanChange.ReturnedAt(today()))), repository.editCalls)

        queue()
        repository.saves.removeFirst().complete(Result.success(open2024.copy(returnedAt = today())))

        assertNull(sut.uiState.value.busyLoanId)
        assertTrue(sut.uiState.value.isLoading)
        assertEquals(listOf(ITEM_ID, ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `mark returned on a returned loan sends nothing`() {
        val sut = createSut()
        answer(Result.success(listOf(returned2023)))

        sut.uiState.value.onMarkReturned(returned2023.id)

        assertTrue(repository.editCalls.isEmpty())
    }

    @Test
    fun `a failed mark returned keeps the list and shows the error`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024)))

        repository.saves.addLast(CompletableDeferred())
        sut.uiState.value.onMarkReturned(open2024.id)
        repository.saves.removeFirst().complete(Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertNull(sut.uiState.value.busyLoanId)
        assertEquals(listOf(open2024), sut.uiState.value.loans)
        assertEquals(NativeText.Arguments(RString.loans_error_http, listOf(500)), sut.uiState.value.actionError)
        assertEquals(1, repository.loansCalls.size)

        sut.uiState.value.onActionErrorShown()

        assertTrue(sut.uiState.value.actionError.isEmpty())
    }

    @Test
    fun `delete asks first and sends nothing until confirmed`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024, returned2023)))

        sut.uiState.value.onDeleteClick(returned2023.id)

        assertEquals(returned2023, sut.uiState.value.deleteConfirm)
        assertTrue(repository.deleteCalls.isEmpty())

        sut.uiState.value.onDeleteDismiss()

        assertNull(sut.uiState.value.deleteConfirm)
        assertTrue(repository.deleteCalls.isEmpty())
    }

    @Test
    fun `a confirmed delete sends the delete and reads again`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024, returned2023)))

        sut.uiState.value.onDeleteClick(returned2023.id)
        repository.deletes.addLast(CompletableDeferred())
        sut.uiState.value.onDeleteConfirm()

        assertNull(sut.uiState.value.deleteConfirm)
        assertEquals(returned2023.id, sut.uiState.value.busyLoanId)
        assertEquals(listOf(returned2023.id), repository.deleteCalls)

        queue()
        repository.deletes.removeFirst().complete(Result.success(Unit))
        answer(Result.success(listOf(open2024)))

        assertNull(sut.uiState.value.busyLoanId)
        assertEquals(listOf(open2024), sut.uiState.value.loans)
        assertEquals(listOf(ITEM_ID, ITEM_ID), repository.loansCalls)
    }

    @Test
    fun `a failed delete keeps the list and shows the error`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024, returned2023)))

        sut.uiState.value.onDeleteClick(open2024.id)
        repository.deletes.addLast(CompletableDeferred())
        sut.uiState.value.onDeleteConfirm()
        repository.deletes.removeFirst().complete(Result.failure(IllegalStateException("parse")))

        assertNull(sut.uiState.value.busyLoanId)
        assertEquals(listOf(open2024, returned2023), sut.uiState.value.loans)
        assertEquals(NativeText.Resource(RString.loans_delete_error_unexpected), sut.uiState.value.actionError)
        assertEquals(1, repository.loansCalls.size)
    }

    @Test
    fun `no second action starts while one runs`() {
        val sut = createSut()
        answer(Result.success(listOf(open2024, open2026)))

        repository.saves.addLast(CompletableDeferred())
        sut.uiState.value.onMarkReturned(open2024.id)
        sut.uiState.value.onMarkReturned(open2026.id)
        sut.uiState.value.onDeleteClick(open2026.id)

        assertEquals(1, repository.editCalls.size)
        assertNull(sut.uiState.value.deleteConfirm)
    }

    private val open2024 = loan(id = "open2024", lentAt = LocalDate(2024, 3, 1))
    private val open2026 = loan(id = "open2026", lentAt = LocalDate(2026, 1, 10))
    private val returned2023 = loan(id = "ret2023", lentAt = LocalDate(2023, 5, 2), returnedAt = LocalDate(2023, 6, 1))
    private val returned2025 = loan(id = "ret2025", lentAt = LocalDate(2025, 2, 2), returnedAt = LocalDate(2025, 2, 20))
}
