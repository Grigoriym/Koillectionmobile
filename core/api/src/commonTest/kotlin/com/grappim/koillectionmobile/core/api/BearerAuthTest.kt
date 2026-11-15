package com.grappim.koillectionmobile.core.api

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.Credentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class BearerAuthTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()
    private val session = FakeSessionStorage(
        credentials = Credentials(username = "tester", password = "hunter2"),
        token = "old-jwt"
    )

    /** [handler] answers every request that is not the login request. */
    private fun client(
        login: suspend MockRequestHandleScope.() -> HttpResponseData = { jsonResponse("""{"token":"new-jwt"}""") },
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            if (request.url.encodedPath.endsWith("/$LOGIN_PATH")) login() else handler(request)
        }
    ) {
        configureKoillectionClient(
            json = json,
            baseUrlProvider = object : BaseUrlProvider {
                override fun getBaseUrl(): String = session.serverUrl
            },
            sessionStorage = session,
            customHeadersStorage = FakeCustomHeadersStorage(),
            logLevel = LogLevel.NONE
        )
    }

    private fun MockRequestHandleScope.jsonResponse(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    // Body copied from the 1.8.5 local test instance (2026-11-15).
    private fun MockRequestHandleScope.expired() =
        jsonResponse("""{"code":401,"message":"Expired JWT Token"}""", HttpStatusCode.Unauthorized)

    private val HttpRequestData.authorization: String? get() = headers[HttpHeaders.Authorization]

    private val HttpRequestData.isLogin: Boolean get() = url.encodedPath.endsWith("/$LOGIN_PATH")

    @Test
    fun `the stored JWT is sent as a bearer header`() = runTest {
        client { jsonResponse("{}") }.get("api/collections")

        assertEquals("Bearer old-jwt", requests.single().authorization)
    }

    @Test
    fun `no header is sent before the first login`() = runTest {
        session.token = null

        client { jsonResponse("{}") }.get("api/collections")

        assertNull(requests.single().authorization)
    }

    @Test
    fun `a 401 logs in once with the stored credentials and repeats the request`() = runTest {
        val client = client { request ->
            if (request.authorization == "Bearer new-jwt") jsonResponse("{}") else expired()
        }

        client.get("api/collections")

        assertEquals(listOf(false, true, false), requests.map { it.isLogin })
        assertEquals("""{"username":"tester","password":"hunter2"}""", requests[1].body.text())
        assertNull(requests[1].authorization)
        assertEquals("Bearer new-jwt", requests[2].authorization)
        assertEquals("https://koi.example/sub/api/collections", requests[2].url.toString())
        assertEquals(listOf("new-jwt"), session.savedTokens)
    }

    @Test
    fun `a second 401 fails as Unauthorized without another login`() = runTest {
        val client = client { expired() }

        val error = assertFailsWith<KoillectionError.Unauthorized> { client.get("api/collections") }

        assertEquals("Expired JWT Token", error.message)
        assertEquals(listOf(false, true, false), requests.map { it.isLogin })
    }

    @Test
    fun `a rejected login fails as Unauthorized and the request is not repeated`() = runTest {
        val client = client(
            login = { jsonResponse("""{"code":401,"message":"Invalid credentials."}""", HttpStatusCode.Unauthorized) }
        ) { expired() }

        val error = assertFailsWith<KoillectionError.Unauthorized> { client.get("api/collections") }

        assertEquals("Invalid credentials.", error.message)
        assertEquals(listOf(false, true), requests.map { it.isLogin })
        assertEquals(emptyList(), session.savedTokens)
    }

    @Test
    fun `a 500 at login keeps its status`() = runTest {
        val client = client(login = { jsonResponse("{}", HttpStatusCode.InternalServerError) }) { expired() }

        val error = assertFailsWith<KoillectionError.Server> { client.get("api/collections") }

        assertEquals(500, error.status)
    }

    @Test
    fun `a 401 without stored credentials fails without a login`() = runTest {
        session.credentials = null

        assertFailsWith<KoillectionError.Unauthorized> { client { expired() }.get("api/collections") }

        assertEquals(listOf(false), requests.map { it.isLogin })
    }

    @Test
    fun `a 401 after another request saved a new JWT repeats with it and does not log in`() = runTest {
        val client = client { request ->
            // The JWT changes while the first attempt is on its way, as if a parallel request
            // had logged in again.
            if (request.authorization == "Bearer old-jwt") {
                session.token = "parallel-jwt"
                expired()
            } else {
                jsonResponse("{}")
            }
        }

        client.get("api/collections")

        assertEquals(listOf("Bearer old-jwt", "Bearer parallel-jwt"), requests.map { it.authorization })
    }

    @Test
    fun `a 401 from the login request itself does not start a login`() = runTest {
        val client = client(
            login = { jsonResponse("""{"code":401,"message":"Invalid credentials."}""", HttpStatusCode.Unauthorized) }
        ) { jsonResponse("{}") }

        assertFailsWith<KoillectionError.Unauthorized> {
            client.post(LOGIN_PATH) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("username" to "tester", "password" to "wrong"))
            }
        }

        assertEquals(1, requests.size)
        assertNull(requests.single().authorization)
    }

    @Test
    fun `a request body is sent again on the repeat`() = runTest {
        val client = client { request ->
            if (request.authorization == "Bearer new-jwt") jsonResponse("{}") else expired()
        }

        client.post("api/items") {
            contentType(ContentTypeLdJson)
            setBody(mapOf("title" to "Dune"))
        }

        assertEquals("""{"title":"Dune"}""", requests.last().body.text())
    }

    private fun OutgoingContent.text(): String = when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        else -> error("unexpected body type ${this::class}")
    }
}
