package com.grappim.koillectionmobile.feature.loans.mapper

import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.dto.LoanDto
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/**
 * The server sends each date as midnight in its own time zone, with the offset
 * (`2026-11-22T00:00:00+01:00`, M10.2). The app keeps the date part as the server wrote it, with
 * no time zone conversion: the web form edits the date only (`LoanType`, `DateType`).
 */
@Single
class LoanMapper {

    fun toDomain(dto: LoanDto): Loan = Loan(
        id = dto.id,
        itemId = dto.item.substringAfterLast('/'),
        lentTo = dto.lentTo,
        lentAt = datePart(dto.lentAt),
        returnedAt = dto.returnedAt?.let(::datePart)
    )

    /** The date as the request bodies send it (`2026-11-22`). */
    fun toApi(date: LocalDate): String = date.toString()

    private fun datePart(value: String): LocalDate = LocalDate.parse(value.substringBefore('T'))
}
