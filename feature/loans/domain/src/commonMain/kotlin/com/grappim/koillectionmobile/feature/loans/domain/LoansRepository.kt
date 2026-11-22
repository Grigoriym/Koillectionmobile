package com.grappim.koillectionmobile.feature.loans.domain

import kotlinx.datetime.LocalDate

/**
 * Access to the loans of an item. A failure is a `KoillectionError` (`core:domain`) inside the
 * [Result].
 */
interface LoansRepository {

    /** The loans of item [itemId], from all pages, in the server's order. */
    suspend fun getLoans(itemId: String): Result<List<Loan>>

    /** Lends item [itemId] to [lentTo] on [lentAt] and returns the loan as the server sent it. */
    suspend fun createLoan(itemId: String, lentTo: String, lentAt: LocalDate): Result<Loan>

    /**
     * Changes the fields of loan [id] that [changes] names, and only those. A
     * [LoanChange.ReturnedAt] with a null value is sent as an explicit `null`. Returns the loan as
     * the server sent it.
     */
    suspend fun editLoan(id: String, changes: List<LoanChange>): Result<Loan>

    suspend fun deleteLoan(id: String): Result<Unit>
}
