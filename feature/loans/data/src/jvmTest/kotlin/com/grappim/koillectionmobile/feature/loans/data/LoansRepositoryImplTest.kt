package com.grappim.koillectionmobile.feature.loans.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.mapper.LoanMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The test copy of the catalogue has no loans, so the bodies are written from the `loan:read`
 * group of Koillection 1.8.5 (`Loan.php`). The server URL has a subpath, so each test also checks
 * that the request path keeps it.
 */
class LoansRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): LoansRepositoryImpl =
        repository(serverUrl = "https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): LoansRepositoryImpl {
        val byPath = routes.toMap()
        val session = FakeSessionStorage(serverUrl = serverUrl, token = "jwt")
        val baseUrlProvider = object : BaseUrlProvider {
            override fun getBaseUrl(): String = session.serverUrl
        }
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                val (status, body) = byPath[request.url.fullPath]
                    ?: (HttpStatusCode.NotFound to """{"title":"An error occurred","detail":"Not Found"}""")
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/ld+json"))
            }
        ) {
            configureKoillectionClient(
                json = json,
                baseUrlProvider = baseUrlProvider,
                sessionStorage = session,
                customHeadersStorage = FakeCustomHeadersStorage(),
                logLevel = LogLevel.NONE
            )
        }
        return LoansRepositoryImpl(httpClient = client, mapper = LoanMapper())
    }

    @Test
    fun `getLoans reads the item's loans path and follows the next link`() = runTest {
        val repository = repository(
            "/sub/api/items/i1/loans" to (
                HttpStatusCode.OK to
                    """{"member":[$OPEN],"totalItems":2,"view":{"next":"/sub/api/items/i1/loans?page=2"}}"""
                ),
            "/sub/api/items/i1/loans?page=2" to (HttpStatusCode.OK to """{"member":[$RETURNED],"totalItems":2}""")
        )

        val result = repository.getLoans("i1").getOrThrow()

        assertEquals(listOf(openLoan, returnedLoan), result)
        assertEquals(
            listOf("GET /sub/api/items/i1/loans", "GET /sub/api/items/i1/loans?page=2"),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getLoans("missing").exceptionOrNull())
        assertIs<KoillectionError.NotFound>(repository.deleteLoan("missing").exceptionOrNull())
    }

    @Test
    fun `createLoan posts the item IRI with lentTo and a date-only lentAt as ld+json`() = runTest {
        val repository = repository("/sub/api/loans" to (HttpStatusCode.Created to OPEN))

        val loan = repository.createLoan("i1", "Anna", LocalDate(2026, 11, 2)).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/loans", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement("""{"item":"/api/items/i1","lentTo":"Anna","lentAt":"2026-11-02"}"""),
            body(request)
        )
        assertEquals(openLoan, loan)
    }

    @Test
    fun `editLoan patches only the changed key as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/loans/l1" to (HttpStatusCode.OK to OPEN))

        repository.editLoan("l1", listOf(LoanChange.LentTo("Bob"))).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/loans/l1", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"lentTo":"Bob"}"""), body(request))
    }

    @Test
    fun `editLoan names each field by its server key and sends dates as yyyy-MM-dd`() = runTest {
        val repository = repository("/sub/api/loans/l1" to (HttpStatusCode.OK to RETURNED))

        val loan = repository.editLoan(
            "l1",
            listOf(
                LoanChange.LentTo("Bob"),
                LoanChange.LentAt(LocalDate(2026, 1, 5)),
                LoanChange.ReturnedAt(LocalDate(2026, 11, 20))
            )
        ).getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"lentTo":"Bob","lentAt":"2026-01-05","returnedAt":"2026-11-20"}"""),
            body(requests.single())
        )
        assertEquals(returnedLoan, loan)
    }

    @Test
    fun `editLoan sends a cleared returnedAt as an explicit null`() = runTest {
        val repository = repository("/sub/api/loans/l1" to (HttpStatusCode.OK to OPEN))

        repository.editLoan("l1", listOf(LoanChange.ReturnedAt(null))).getOrThrow()

        assertEquals(json.parseToJsonElement("""{"returnedAt":null}"""), body(requests.single()))
    }

    @Test
    fun `deleteLoan sends DELETE to the loan's path`() = runTest {
        val repository = repository("/sub/api/loans/l1" to (HttpStatusCode.NoContent to ""))

        repository.deleteLoan("l1").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/loans/l1", request.url.fullPath)
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `every call keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/items/i1/loans" to (HttpStatusCode.OK to """{"member":[$OPEN]}"""),
            "/api/loans" to (HttpStatusCode.Created to OPEN),
            "/api/loans/l1" to (HttpStatusCode.OK to OPEN)
        )

        repository.getLoans("i1").getOrThrow()
        repository.createLoan("i1", "Anna", LocalDate(2026, 11, 2)).getOrThrow()
        repository.editLoan("l1", listOf(LoanChange.LentTo("Anna"))).getOrThrow()
        repository.deleteLoan("l1").getOrThrow()

        assertEquals(
            listOf(
                "GET /api/items/i1/loans",
                "POST /api/loans",
                "PATCH /api/loans/l1",
                "DELETE /api/loans/l1"
            ),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
        assertEquals(
            json.parseToJsonElement("""{"item":"/api/items/i1","lentTo":"Anna","lentAt":"2026-11-02"}"""),
            body(requests[1])
        )
    }

    private fun body(request: HttpRequestData): JsonElement =
        json.parseToJsonElement((request.body as TextContent).text)

    private val openLoan = Loan(
        id = "l1",
        itemId = "i1",
        lentTo = "Anna",
        lentAt = LocalDate(2026, 11, 2),
        returnedAt = null
    )
    private val returnedLoan = Loan(
        id = "l2",
        itemId = "i1",
        lentTo = "Bob",
        lentAt = LocalDate(2026, 1, 5),
        returnedAt = LocalDate(2026, 11, 20)
    )

    private companion object {
        /** Bodies in the `loan:read` group. */
        const val OPEN = """{"id":"l1","item":"/api/items/i1","lentTo":"Anna",""" +
            """"lentAt":"2026-11-02T00:00:00+00:00","returnedAt":null,"owner":"/api/users/u"}"""
        const val RETURNED = """{"id":"l2","item":"/api/items/i1","lentTo":"Bob",""" +
            """"lentAt":"2026-01-05T00:00:00+00:00","returnedAt":"2026-11-20T00:00:00+00:00",""" +
            """"owner":"/api/users/u"}"""
    }
}
