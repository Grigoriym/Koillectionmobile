package com.grappim.koillectionmobile.core.api

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.Credentials
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The bodies are copied from the 1.8.5 test instance and an nginx front over it (2026-11-16,
 * `docs/local-info.md`, "Proxy front").
 */
class ProxyRejectionTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        baseUrl: String,
        session: FakeSessionStorage = FakeSessionStorage(serverUrl = baseUrl),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient = HttpClient(
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
    }

    private fun MockRequestHandleScope.nginx(status: HttpStatusCode, vararg extra: Pair<String, String>) = respond(
        "<html>\r\n<head><title>${status.value} ${status.description}</title></head>\r\n</html>",
        status,
        headersOf(
            HttpHeaders.ContentType to listOf("text/html"),
            *extra.map { (name, value) -> name to listOf(value) }.toTypedArray()
        )
    )

    private fun MockRequestHandleScope.koillection401(message: String) = respond(
        """{"code":401,"message":"$message"}""",
        HttpStatusCode.Unauthorized,
        headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            HttpHeaders.WWWAuthenticate to listOf("Bearer")
        )
    )

    private suspend fun proxyRejectionOf(
        baseUrl: String,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): KoillectionError.ProxyRejection {
        val client = client(baseUrl, handler = handler)
        return assertFailsWith<KoillectionError.ProxyRejection> { client.get("api/collections") }
    }

    // The predicate.

    @Test
    fun `the JWT layer's 401 bodies are Koillection's own`() {
        listOf("JWT Token not found", "Invalid JWT Token", "Expired JWT Token", "Invalid credentials.").forEach {
            assertFalse(isProxyRejection(401, """{"code":401,"message":"$it"}""", null, HOST), it)
        }
    }

    @Test
    fun `a 401 with an HTML or empty or other JSON body is a proxy`() {
        listOf("<html><title>401 Authorization Required</title></html>", "", "{}", "[401]")
            .forEach { assertTrue(isProxyRejection(401, it, null, HOST), it) }
    }

    @Test
    fun `a 407 is a proxy`() {
        assertTrue(isProxyRejection(407, "", null, HOST))
    }

    @Test
    fun `a redirect to another host is a proxy`() {
        assertTrue(isProxyRejection(302, "", "https://auth.example.com/login?rd=/api/collections", HOST))
        assertTrue(isProxyRejection(307, "", "//auth.example.com/login", HOST))
    }

    @Test
    fun `a redirect on the same host or a relative one is not a proxy`() {
        listOf("https://KOI.example/api/collections", "/login", "login", null).forEach {
            assertFalse(isProxyRejection(301, "", it, HOST), it.toString())
        }
    }

    @Test
    fun `other statuses are not a proxy`() {
        assertFalse(isProxyRejection(403, "<html></html>", null, HOST))
        assertFalse(isProxyRejection(502, "<html></html>", null, HOST))
    }

    // The client, at a subpath and at the root path (CLAUDE.md: a URL check needs a root-path case).

    @Test
    fun `an nginx 401 fails as ProxyRejection`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            assertEquals(401, proxyRejectionOf(baseUrl) { nginx(HttpStatusCode.Unauthorized) }.status, baseUrl)
        }
    }

    @Test
    fun `an nginx auth_basic 401 fails as ProxyRejection`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            val error = proxyRejectionOf(baseUrl) {
                nginx(HttpStatusCode.Unauthorized, HttpHeaders.WWWAuthenticate to "Basic realm=\"test\"")
            }
            assertEquals(401, error.status, baseUrl)
        }
    }

    @Test
    fun `a 407 fails as ProxyRejection`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            assertEquals(407, proxyRejectionOf(baseUrl) { nginx(HttpStatusCode.ProxyAuthenticationRequired) }.status)
        }
    }

    @Test
    fun `a GET redirect to another host fails as ProxyRejection and is not followed`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            requests.clear()
            val error = proxyRejectionOf(baseUrl) {
                nginx(HttpStatusCode.Found, HttpHeaders.Location to OFF_HOST_LOGIN)
            }
            assertEquals(302, error.status)
            assertEquals(listOf("koi.example"), requests.map { it.url.host }, baseUrl)
        }
    }

    @Test
    fun `a redirect on the same host fails as Http and is not followed`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            requests.clear()
            val client = client(baseUrl) {
                nginx(HttpStatusCode.MovedPermanently, HttpHeaders.Location to "https://koi.example/api/collections")
            }
            val error = assertFailsWith<KoillectionError.Http> { client.get("api/collections") }
            assertEquals(301, error.status)
            assertEquals(1, requests.size, baseUrl)
        }
    }

    @Test
    fun `Koillection's own 401 still fails as Unauthorized`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            val client = client(baseUrl) { koillection401("JWT Token not found") }
            assertFailsWith<KoillectionError.Unauthorized> { client.get("api/collections") }
        }
    }

    @Test
    fun `a proxy 401 does not start a re-login and keeps the session`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            requests.clear()
            val session = FakeSessionStorage(
                serverUrl = baseUrl,
                credentials = Credentials(username = "tester", password = "hunter2"),
                token = "old-jwt"
            )
            val client = client(baseUrl, session) { nginx(HttpStatusCode.Unauthorized) }

            assertFailsWith<KoillectionError.ProxyRejection> { client.get("api/collections") }

            assertEquals(1, requests.size, baseUrl)
            assertEquals(emptyList(), session.savedTokens)
            assertNotNull(session.credentials)
            assertEquals("old-jwt", session.token)
        }
    }

    @Test
    fun `a proxy 401 on the re-login does not clear the session`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            val session = FakeSessionStorage(
                serverUrl = baseUrl,
                credentials = Credentials(username = "tester", password = "hunter2"),
                token = "old-jwt"
            )
            val client = client(baseUrl, session) { request ->
                if (request.url.encodedPath.endsWith(LOGIN_PATH)) {
                    nginx(HttpStatusCode.Unauthorized)
                } else {
                    koillection401("Expired JWT Token")
                }
            }

            assertFailsWith<KoillectionError.ProxyRejection> { client.get("api/collections") }

            assertNotNull(session.credentials, baseUrl)
        }
    }

    @Test
    fun `a proxy 401 at login fails as ProxyRejection`() = runTest {
        BASE_URLS.forEach { baseUrl ->
            val client = client(baseUrl) { nginx(HttpStatusCode.Unauthorized) }
            assertFailsWith<KoillectionError.ProxyRejection> { client.post(LOGIN_PATH) }
        }
    }

    private companion object {
        const val HOST = "koi.example"
        const val OFF_HOST_LOGIN = "https://auth.example.com/login?rd=/api/collections"
        val BASE_URLS = listOf("https://koi.example/sub", "https://koi.example")
    }
}
