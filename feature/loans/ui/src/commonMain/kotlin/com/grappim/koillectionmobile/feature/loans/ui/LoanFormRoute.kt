package com.grappim.koillectionmobile.feature.loans.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The loan form of item [itemId]: a new loan when [loanId] is null, else the edit form of that
 * loan. The form reads the loan again, not a snapshot of the section. Registered in the shell's
 * `navKeySerializersModule`.
 */
@Serializable
data class LoanFormRoute(val itemId: String, val loanId: String? = null) : NavKey
