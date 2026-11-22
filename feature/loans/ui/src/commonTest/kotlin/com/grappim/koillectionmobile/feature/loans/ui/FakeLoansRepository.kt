package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/**
 * Each loans read takes the first queued answer in [loans]; each create or edit takes the first
 * queued answer in [saves]; each delete takes the first queued answer in [deletes]. Queue one
 * before the call.
 */
class FakeLoansRepository : LoansRepository {
    val loans = ArrayDeque<CompletableDeferred<Result<List<Loan>>>>()
    val loansCalls = mutableListOf<String>()
    val saves = ArrayDeque<CompletableDeferred<Result<Loan>>>()
    val createCalls = mutableListOf<CreateCall>()
    val editCalls = mutableListOf<Pair<String, List<LoanChange>>>()
    val deletes = ArrayDeque<CompletableDeferred<Result<Unit>>>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun getLoans(itemId: String): Result<List<Loan>> {
        loansCalls += itemId
        return loans.first().await()
    }

    override suspend fun createLoan(itemId: String, lentTo: String, lentAt: LocalDate): Result<Loan> {
        createCalls += CreateCall(itemId, lentTo, lentAt)
        return saves.first().await()
    }

    override suspend fun editLoan(id: String, changes: List<LoanChange>): Result<Loan> {
        editCalls += id to changes
        return saves.first().await()
    }

    override suspend fun deleteLoan(id: String): Result<Unit> {
        deleteCalls += id
        return deletes.first().await()
    }

    data class CreateCall(val itemId: String, val lentTo: String, val lentAt: LocalDate)
}

internal fun loan(id: String, lentAt: LocalDate, returnedAt: LocalDate? = null) =
    Loan(id = id, itemId = ITEM_ID, lentTo = "Friend $id", lentAt = lentAt, returnedAt = returnedAt)

internal const val ITEM_ID = "item-1"
