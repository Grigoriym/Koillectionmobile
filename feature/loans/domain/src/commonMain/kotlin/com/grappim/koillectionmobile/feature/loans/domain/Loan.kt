package com.grappim.koillectionmobile.feature.loans.domain

import kotlinx.datetime.LocalDate

/**
 * A loan of item [itemId] to [lentTo]. Both dates are date only (plan §5 decision 13).
 * [returnedAt] is null while the loan is not returned.
 */
data class Loan(
    val id: String,
    val itemId: String,
    val lentTo: String,
    val lentAt: LocalDate,
    val returnedAt: LocalDate?
)

/** One field of a loan that an edit changes. Each one is a key of the `PATCH` body. */
sealed interface LoanChange {
    data class LentTo(val value: String) : LoanChange

    data class LentAt(val value: LocalDate) : LoanChange

    /** A null [value] clears the returned date: the loan is not returned again. */
    data class ReturnedAt(val value: LocalDate?) : LoanChange
}
