package com.grappim.koillectionmobile.feature.loans.dto

import kotlinx.serialization.Serializable

/**
 * A Loan as `GET /api/loans/{id}` and `/api/items/{id}/loans` send it (`loan:read` group,
 * Koillection 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [item] is an IRI (`/api/items/<id>`): `Item` has no property in the `loan:read` group.
 * - [lentAt] and [returnedAt] are date-times (`DATETIME_IMMUTABLE` columns), as the server
 *   formats them. [returnedAt] is null while the loan is not returned.
 */
@Serializable
data class LoanDto(
    val id: String,
    val item: String,
    val lentTo: String,
    val lentAt: String,
    val returnedAt: String? = null
)

/**
 * The body of `POST /api/loans` (`loan:write` group). All three are required
 * (`#[Assert\NotBlank]`).
 *
 * - [item] is an IRI (`/api/items/<id>`).
 * - [lentAt] is a date (`2026-11-22`).
 */
@Serializable
data class NewLoanDto(val item: String, val lentTo: String, val lentAt: String)

/**
 * The keys of `PATCH /api/loans/{id}` (`loan:write` group). The repository builds the body as a
 * JSON object from only the changed keys, so a cleared [RETURNED_AT] can be an explicit `null`.
 */
object LoanPatchKeys {
    const val LENT_TO = "lentTo"
    const val LENT_AT = "lentAt"
    const val RETURNED_AT = "returnedAt"
}
