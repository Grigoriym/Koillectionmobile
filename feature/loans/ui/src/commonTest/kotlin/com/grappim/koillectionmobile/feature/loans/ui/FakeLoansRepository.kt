package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/** Each loans read takes the first queued answer in [loans]. Queue one before the call. */
class FakeLoansRepository : LoansRepository {
    val loans = ArrayDeque<CompletableDeferred<Result<List<Loan>>>>()
    val loansCalls = mutableListOf<String>()

    override suspend fun getLoans(itemId: String): Result<List<Loan>> {
        loansCalls += itemId
        return loans.first().await()
    }

    override suspend fun createLoan(itemId: String, lentTo: String, lentAt: LocalDate): Result<Loan> = error("not used")

    override suspend fun editLoan(id: String, changes: List<LoanChange>): Result<Loan> = error("not used")

    override suspend fun deleteLoan(id: String): Result<Unit> = error("not used")
}

internal fun loan(id: String, lentAt: LocalDate, returnedAt: LocalDate? = null) =
    Loan(id = id, itemId = ITEM_ID, lentTo = "Friend $id", lentAt = lentAt, returnedAt = returnedAt)

internal const val ITEM_ID = "item-1"
