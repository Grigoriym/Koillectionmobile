package com.grappim.koillectionmobile.core.api

import com.grappim.kit.customheaders.CustomHeader
import com.grappim.kit.logger.KitLogger
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.domain.KoillectionError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KoillectionHttpClientTest {

    @Serializable
    private data class TitleDto(val id: String, val title: String)

    @Serializable
    private data class Credentials(val username: String, val password: String)

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        baseUrl: String = "https://koi.example/sub",
        headers: List<CustomHeader> = emptyList(),
        logLevel: LogLevel = LogLevel.NONE,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { ok("{}") }
    ): HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            handler(request)
        }
    ) {
        configureKoillectionClient(
            json = json,
            baseUrlProvider = object : BaseUrlProvider {
                override fun getBaseUrl(): String = baseUrl
            },
            sessionStorage = FakeSessionStorage(),
            customHeadersStorage = FakeCustomHeadersStorage(headers),
            logLevel = logLevel
        )
    }

    private fun MockRequestHandleScope.ok(body: String, type: ContentType = ContentTypeLdJson) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, type.toString()))

    private fun MockRequestHandleScope.error(status: HttpStatusCode, body: String, type: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, type))

    private suspend fun failureOf(status: HttpStatusCode, body: String, type: String): KoillectionError {
        val client = client { error(status, body, type) }
        return assertFailsWith<KoillectionError> { client.get("api/collections") }
    }

    @AfterTest
    fun tearDown() {
        KitLogger.uninstall()
    }

    @Test
    fun `a relative path resolves under a base URL without a trailing slash`() = runTest {
        client(baseUrl = "https://koi.example/sub").get("api/collections")

        assertEquals("https://koi.example/sub/api/collections", requests.single().url.toString())
    }

    @Test
    fun `requests ask for ld+json`() = runTest {
        client().get("api/collections")

        assertEquals(listOf("application/ld+json"), requests.single().headers.getAll(HttpHeaders.Accept))
    }

    @Test
    fun `an ld+json body decodes and unknown keys are ignored`() = runTest {
        val body = """{"@id":"/api/collections/c1","@type":"Collection","id":"c1","title":"Books","color":"x"}"""

        val dto = client { ok(body) }.get("api/collections/c1").body<TitleDto>()

        assertEquals(TitleDto(id = "c1", title = "Books"), dto)
    }

    @Test
    fun `an ld+json body is serialized`() = runTest {
        client().post("api/items") {
            contentType(ContentTypeLdJson)
            setBody(TitleDto(id = "i1", title = "Dune"))
        }

        val content = requests.single().body
        assertEquals(ContentTypeLdJson, content.contentType?.withoutParameters())
        assertEquals("""{"id":"i1","title":"Dune"}""", content.text())
    }

    @Test
    fun `a PATCH body is sent as merge-patch+json`() = runTest {
        client().patch("api/items/i1") {
            contentType(ContentTypeMergePatchJson)
            setBody(TitleDto(id = "i1", title = "Dune"))
        }

        val content = requests.single().body
        assertEquals(ContentTypeMergePatchJson, content.contentType?.withoutParameters())
        assertEquals("""{"id":"i1","title":"Dune"}""", content.text())
    }

    @Test
    fun `custom headers go to the server`() = runTest {
        client(headers = listOf(CustomHeader("X-Proxy-Token", "secret"))).get("api/collections")

        assertEquals("secret", requests.single().headers["X-Proxy-Token"])
    }

    // Error bodies below are copied from the 1.8.5 local test instance (2026-11-15).

    @Test
    fun `401 from the JWT layer maps to Unauthorized with its message`() = runTest {
        val error = failureOf(
            HttpStatusCode.Unauthorized,
            """{"code":401,"message":"Invalid credentials."}""",
            "application/json"
        )

        assertIs<KoillectionError.Unauthorized>(error)
        assertEquals("Invalid credentials.", error.message)
    }

    @Test
    fun `404 problem+json maps to NotFound with its detail`() = runTest {
        val error = failureOf(
            HttpStatusCode.NotFound,
            """{"@context":"/api/contexts/Error","@type":"Error","title":"An error occurred",""" +
                """"detail":"Not Found","status":404,"type":"/errors/404","description":"Not Found"}""",
            "application/problem+json; charset=utf-8"
        )

        assertIs<KoillectionError.NotFound>(error)
        assertEquals("Not Found", error.message)
    }

    @Test
    fun `422 maps to Validation with its detail`() = runTest {
        val error = failureOf(
            HttpStatusCode.UnprocessableEntity,
            """{"@type":"ConstraintViolation","status":422,"violations":[{"propertyPath":"title",""" +
                """"message":"This value should not be blank."}],"detail":"title: This value should not be blank.",""" +
                """"title":"An error occurred"}""",
            "application/problem+json; charset=utf-8"
        )

        assertIs<KoillectionError.Validation>(error)
        assertEquals("title: This value should not be blank.", error.message)
    }

    @Test
    fun `older Hydra error bodies still give a message`() = runTest {
        val error = failureOf(HttpStatusCode.BadRequest, """{"hydra:description":"Bad input"}""", "application/ld+json")

        assertIs<KoillectionError.Validation>(error)
        assertEquals("Bad input", error.message)
    }

    @Test
    fun `500 with a non-JSON body maps to Server with no message`() = runTest {
        val error = failureOf(HttpStatusCode.InternalServerError, "<html>oops</html>", "text/html")

        assertIs<KoillectionError.Server>(error)
        assertEquals(500, error.status)
        assertNull(error.message)
    }

    @Test
    fun `any other status maps to Http with the status`() = runTest {
        val error = failureOf(HttpStatusCode.Forbidden, """{"detail":"Access Denied."}""", "application/problem+json")

        assertIs<KoillectionError.Http>(error)
        assertEquals(403, error.status)
        assertEquals("Access Denied.", error.message)
    }

    @Test
    fun `a network failure maps to Network`() = runTest {
        val client = client { throw IOException("Unable to resolve host") }

        val error = assertFailsWith<KoillectionError.Network> { client.get("api/collections") }

        // Not the same instance: coroutines stack-trace recovery copies the exception.
        assertIs<IOException>(error.cause)
        assertEquals("Unable to resolve host", error.message)
    }

    @Test
    fun `the log holds no password and no JWT`() = runTest {
        val lines = mutableListOf<String>()
        KitLogger.install(
            object : KitLogger {
                override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
                    lines += message()
                }
            }
        )
        val jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VybmFtZSI6InRlc3RlciJ9.c2lnbmF0dXJl"
        val client = client(logLevel = LogLevel.ALL) { ok("""{"token":"$jwt"}""", ContentType.Application.Json) }

        val response: HttpResponse = client.post("api/authentication_token") {
            contentType(ContentType.Application.Json)
            bearerAuth(jwt)
            setBody(Credentials(username = "tester", password = "hunter2"))
        }
        response.body<Map<String, String>>()

        val log = lines.joinToString("\n")
        assertTrue(log.contains("authentication_token"), "the log ran at all: $log")
        assertFalse(log.contains("hunter2"), log)
        assertFalse(log.contains(jwt), log)
        assertFalse(log.contains("eyJ"), log)
    }

    @Test
    fun `the log names a custom header but hides its value`() = runTest {
        val lines = mutableListOf<String>()
        KitLogger.install(
            object : KitLogger {
                override fun log(priority: LogPriority, tag: String?, throwable: Throwable?, message: () -> String) {
                    lines += message()
                }
            }
        )

        client(headers = listOf(CustomHeader("X-Proxy-Token", "pr0xy-s3cret")), logLevel = LogLevel.ALL)
            .get("api/collections")

        val log = lines.joinToString("\n")
        assertEquals("pr0xy-s3cret", requests.single().headers["X-Proxy-Token"])
        assertTrue(log.contains("X-Proxy-Token"), log)
        assertFalse(log.contains("pr0xy-s3cret"), log)
    }

    private fun OutgoingContent.text(): String = when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        else -> error("unexpected body type ${this::class}")
    }
}
