package com.grappim.koillectionmobile.feature.loans.ui

import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** The fields of the loan form. [lentTo] is as the user typed it; the server gets it trimmed. */
@Serializable
data class LoanFormValues(val lentTo: String = "", val lentAt: LocalDate, val returnedAt: LocalDate? = null) {

    /** The server needs a name (`NotBlank` in `Loan.php`). */
    val canSave: Boolean
        get() = lentTo.isNotBlank()

    /**
     * The fields whose value differs from [original], with the new value. A null returned date
     * clears it: the server takes an explicit `null` (M10.2, plan §2).
     */
    fun changesFrom(original: LoanFormValues): List<LoanChange> = buildList {
        if (lentTo.trim() != original.lentTo.trim()) add(LoanChange.LentTo(lentTo.trim()))
        if (lentAt != original.lentAt) add(LoanChange.LentAt(lentAt))
        if (returnedAt != original.returnedAt) add(LoanChange.ReturnedAt(returnedAt))
    }
}

internal fun Loan.toFormValues(): LoanFormValues =
    LoanFormValues(lentTo = lentTo, lentAt = lentAt, returnedAt = returnedAt)
