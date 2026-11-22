package com.grappim.koillectionmobile.feature.loans.mapper

import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.dto.LoanDto
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** The test copy of the catalogue has no loans, so the values follow the `loan:read` group of `Loan.php`. */
class LoanMapperTest {

    private val mapper = LoanMapper()

    @Test
    fun `toDomain takes the date part of each date-time and the item id from its IRI`() {
        val dto = LoanDto(
            id = "l1",
            item = "/api/items/i1",
            lentTo = "Anna",
            lentAt = "2026-11-02T00:00:00+00:00",
            returnedAt = "2026-11-20T00:00:00+00:00"
        )

        assertEquals(
            Loan(
                id = "l1",
                itemId = "i1",
                lentTo = "Anna",
                lentAt = LocalDate(2026, 11, 2),
                returnedAt = LocalDate(2026, 11, 20)
            ),
            mapper.toDomain(dto)
        )
    }

    /** A late time with a far offset must not move the date: the mapper does no time zone conversion. */
    @Test
    fun `toDomain keeps the server's date at a late time and a far offset`() {
        val dto = LoanDto(id = "l1", item = "/api/items/i1", lentTo = "Anna", lentAt = "2026-11-02T23:30:00-11:00")

        assertEquals(LocalDate(2026, 11, 2), mapper.toDomain(dto).lentAt)
    }

    @Test
    fun `toDomain maps a missing returnedAt to null`() {
        val dto = LoanDto(id = "l1", item = "/api/items/i1", lentTo = "Anna", lentAt = "2026-11-02T00:00:00+00:00")

        assertEquals(null, mapper.toDomain(dto).returnedAt)
    }

    @Test
    fun `toDomain reads a date without a time`() {
        val dto = LoanDto(id = "l1", item = "/api/items/i1", lentTo = "Anna", lentAt = "2026-11-02")

        assertEquals(LocalDate(2026, 11, 2), mapper.toDomain(dto).lentAt)
    }

    @Test
    fun `toApi writes the date as yyyy-MM-dd`() {
        assertEquals("2026-01-05", mapper.toApi(LocalDate(2026, 1, 5)))
    }
}
