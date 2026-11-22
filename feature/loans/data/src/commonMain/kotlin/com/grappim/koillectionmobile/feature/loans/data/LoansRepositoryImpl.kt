package com.grappim.koillectionmobile.feature.loans.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import com.grappim.koillectionmobile.feature.loans.dto.LoanDto
import com.grappim.koillectionmobile.feature.loans.dto.LoanPatchKeys
import com.grappim.koillectionmobile.feature.loans.dto.NewLoanDto
import com.grappim.koillectionmobile.feature.loans.mapper.LoanMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. Paths are relative to the stored server URL. A request
 * body names the item by IRI (`/api/items/<id>`).
 */
@Single(binds = [LoansRepository::class])
internal class LoansRepositoryImpl(private val httpClient: HttpClient, private val mapper: LoanMapper) :
    LoansRepository {

    override suspend fun getLoans(itemId: String): Result<List<Loan>> = resultOf {
        httpClient.getAllHydraPages<LoanDto>("$ITEMS_PATH/$itemId/loans").map(mapper::toDomain)
    }

    override suspend fun createLoan(itemId: String, lentTo: String, lentAt: LocalDate): Result<Loan> = resultOf {
        val dto = httpClient.post(LOANS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(NewLoanDto(item = "/$ITEMS_PATH/$itemId", lentTo = lentTo, lentAt = mapper.toApi(lentAt)))
        }.body<LoanDto>()
        mapper.toDomain(dto)
    }

    /** A [JsonObject] body, so that a null value is written as `null` and an absent key is left out. */
    override suspend fun editLoan(id: String, changes: List<LoanChange>): Result<Loan> = resultOf {
        val body = JsonObject(changes.associate { it.entry() })
        val dto = httpClient.patch("$LOANS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(body)
        }.body<LoanDto>()
        mapper.toDomain(dto)
    }

    override suspend fun deleteLoan(id: String): Result<Unit> = resultOf {
        httpClient.delete("$LOANS_PATH/$id")
        Unit
    }

    private fun LoanChange.entry() = when (this) {
        is LoanChange.LentTo -> LoanPatchKeys.LENT_TO to JsonPrimitive(value)
        is LoanChange.LentAt -> LoanPatchKeys.LENT_AT to JsonPrimitive(mapper.toApi(value))
        is LoanChange.ReturnedAt -> LoanPatchKeys.RETURNED_AT to returnedAtValue(value)
    }

    private fun returnedAtValue(value: LocalDate?) = if (value == null) {
        JsonNull
    } else {
        JsonPrimitive(mapper.toApi(value))
    }
}

private const val ITEMS_PATH = "api/items"
private const val LOANS_PATH = "api/loans"
