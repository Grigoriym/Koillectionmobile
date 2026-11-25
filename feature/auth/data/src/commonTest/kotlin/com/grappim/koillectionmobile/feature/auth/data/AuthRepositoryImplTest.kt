package com.grappim.koillectionmobile.feature.auth.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.Credentials
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** A session for another server, as after an earlier login. */
    private val session = FakeSessionStorage(
        serverUrl = "https://old.example",
        credentials = Credentials(username = "old-user", password = "old-password"),
        token = "old-jwt"
    )

    private fun repository(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        AuthRepositoryImpl(
            httpClient = HttpClient(
                MockEngine { request ->
                    requests += request
                    handler(request)
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
            },
            sessionStorage = session,
            json = json
        )

    private fun MockRequestHandleScope.respondWith(
        body: String,
        status: HttpStatusCode,
        contentType: String = ContentType.Application.Json.toString()
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, contentType))

    private suspend fun AuthRepositoryImpl.logIn() =
        logIn(serverUrl = " https://koi.example/sub ", username = "tester", password = "hunter2")

    private fun assertSessionUnchanged() {
        assertEquals("https://old.example", session.serverUrl)
        assertEquals(Credentials(username = "old-user", password = "old-password"), session.credentials)
        assertEquals("old-jwt", session.token)
    }

    @Test
    fun `a login stores the URL and the credentials and the JWT`() = runTest {
        val repository = repository { respondWith("""{"token":"new-jwt"}""", HttpStatusCode.OK) }

        val result = repository.logIn()

        assertEquals(LoginResult.LoggedIn, result)
        assertEquals("https://koi.example/sub", session.serverUrl)
        assertEquals(Credentials(username = "tester", password = "hunter2"), session.credentials)
        assertEquals("new-jwt", session.token)
    }

    @Test
    fun `the login request is a JSON POST to the typed server without the old JWT`() = runTest {
        repository { respondWith("""{"token":"new-jwt"}""", HttpStatusCode.OK) }.logIn()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://koi.example/sub/api/authentication_token", request.url.toString())
        assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
        assertEquals("""{"username":"tester","password":"hunter2"}""", request.body.text())
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a 401 is InvalidCredentials and leaves the session unchanged`() = runTest {
        // Body as in fixtures/koillection-1.8.5/login_401_wrong_password.json.
        val repository = repository {
            respondWith("""{"code":401,"message":"Invalid credentials."}""", HttpStatusCode.Unauthorized)
        }

        assertEquals(LoginResult.InvalidCredentials, repository.logIn())
        assertEquals(1, requests.size)
        assertSessionUnchanged()
    }

    @Test
    fun `a proxy 401 is Failed with ProxyRejection and not InvalidCredentials`() = runTest {
        // nginx `return 401` in front of the 1.8.5 test instance (2026-11-16).
        val repository = repository {
            respondWith(
                body = "<html><head><title>401 Authorization Required</title></head></html>",
                status = HttpStatusCode.Unauthorized,
                contentType = "text/html"
            )
        }

        val result = repository.logIn()

        val error = assertIs<LoginResult.Failed>(result).error
        assertEquals(401, assertIs<KoillectionError.ProxyRejection>(error).status)
        assertSessionUnchanged()
    }

    @Test
    fun `a 500 is ServerJwtFailure`() = runTest {
        val repository = repository {
            respondWith("""{"code":500,"message":"Internal Server Error"}""", HttpStatusCode.InternalServerError)
        }

        assertEquals(LoginResult.ServerJwtFailure, repository.logIn())
        assertSessionUnchanged()
    }

    @Test
    fun `a network failure is Unreachable`() = runTest {
        val repository = repository { throw IOException("Unable to resolve host") }

        assertEquals(LoginResult.Unreachable, repository.logIn())
        assertSessionUnchanged()
    }

    @Test
    fun `a 404 is NotKoillection`() = runTest {
        // Koillection 1.8.5's own answer to a wrong subpath (local test instance, 2026-11-15).
        val repository = repository {
            respondWith(
                """{"title":"An error occurred","status":404,"detail":"Not Found"}""",
                HttpStatusCode.NotFound,
                "application/ld+json"
            )
        }

        assertEquals(LoginResult.NotKoillection, repository.logIn())
        assertSessionUnchanged()
    }

    @Test
    fun `a 405 is NotKoillection`() = runTest {
        val repository = repository { respondWith("", HttpStatusCode.MethodNotAllowed, "text/html") }

        assertEquals(LoginResult.NotKoillection, repository.logIn())
    }

    @Test
    fun `an HTML page with 200 is NotKoillection`() = runTest {
        val repository = repository { respondWith("<html>Welcome</html>", HttpStatusCode.OK, "text/html") }

        assertEquals(LoginResult.NotKoillection, repository.logIn())
        assertSessionUnchanged()
    }

    @Test
    fun `a JSON body without a token is NotKoillection`() = runTest {
        val repository = repository { respondWith("""{"status":"ok"}""", HttpStatusCode.OK) }

        assertEquals(LoginResult.NotKoillection, repository.logIn())
        assertSessionUnchanged()
    }

    @Test
    fun `another server error is Failed with the error`() = runTest {
        val repository = repository { respondWith("Bad Gateway", HttpStatusCode.BadGateway, "text/html") }

        val result = assertIs<LoginResult.Failed>(repository.logIn())

        assertEquals(502, assertIs<KoillectionError.Server>(result.error).status)
        assertSessionUnchanged()
    }

    @Test
    fun `an invalid URL sends no request`() = runTest {
        val repository = repository { error("no request expected") }

        val result = repository.logIn(serverUrl = "koi.example", username = "tester", password = "hunter2")

        assertEquals(LoginResult.InvalidServerUrl, result)
        assertEquals(0, requests.size)
        assertSessionUnchanged()
    }

    @Test
    fun `a server URL needs an http or https scheme and a host`() {
        listOf("https://koi.example", "http://192.168.1.5:8290/koillection/", "HTTPS://Koi.Example")
            .forEach { assertTrue(isServerUrl(it), it) }
        listOf("", "koi.example", "koi.example/sub", "ftp://koi.example", "https://", "https:///sub")
            .forEach { assertFalse(isServerUrl(it), it) }
    }

    @Test
    fun `log out clears the session sends no request and keeps the server URL`() = runTest {
        val repository = repository { error("no request expected") }

        repository.logOut()

        assertNull(session.credentials)
        assertNull(session.token)
        assertEquals("https://old.example", repository.storedServerUrl())
        assertEquals(0, requests.size)
    }

    private fun OutgoingContent.text(): String = when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        else -> error("unexpected body type ${this::class}")
    }
}
