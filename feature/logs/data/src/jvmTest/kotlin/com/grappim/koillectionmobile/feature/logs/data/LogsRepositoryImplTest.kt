package com.grappim.koillectionmobile.feature.logs.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.logs.mapper.LogMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Bodies are the M13.1 fixtures from the 1.8.5 local test instance. The server URL has a subpath,
 * so the `view` links in the bodies are rewritten to hold it, as a server under `/sub` sends them.
 */
class LogsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    private fun fixture(name: String, prefix: String = "/sub"): String =
        File("../../../fixtures/koillection-1.8.5/$name").readText().replace("\"/api/", "\"$prefix/api/")

    private fun repository(
        serverUrl: String = "https://koi.example/sub",
        vararg routes: Pair<String, String>
    ): LogsRepositoryImpl {
        val byPath = routes.toMap()
        val session = FakeSessionStorage(serverUrl = serverUrl, token = "jwt")
        val baseUrlProvider = object : BaseUrlProvider {
            override fun getBaseUrl(): String = session.serverUrl
        }
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                val body = byPath[request.url.fullPath]
                if (body == null) {
                    respond(
                        """{"title":"An error occurred","detail":"Not Found"}""",
                        HttpStatusCode.NotFound,
                        headersOf(HttpHeaders.ContentType, "application/ld+json")
                    )
                } else {
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/ld+json"))
                }
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
        return LogsRepositoryImpl(httpClient = client, mapper = LogMapper())
    }

    private fun paths() = requests.map { "${it.method.value} ${it.url.fullPath}" }

    @Test
    fun `the first call reads the list then its last page and returns it newest first`() = runTest {
        val repository = repository(
            routes = arrayOf(
                "/sub/api/logs" to fixture("logs_page_first.json"),
                "/sub/api/logs?page=35" to fixture("logs_page_last.json")
            )
        )

        val page = repository.getLogs(null).getOrThrow()

        assertEquals(listOf("GET /sub/api/logs", "GET /sub/api/logs?page=35"), paths())
        assertEquals(20, page.logs.size)
        assertEquals("01a2122a-2671-7e58-b2f5-1518ee70917e", page.logs.first().id)
        assertEquals("01a2122a-2114-7977-8b13-e11896a64aa3", page.logs.last().id)
        assertEquals(page.logs.sortedByDescending { it.id }, page.logs)
        assertEquals("/sub/api/logs?page=34", page.nextPath)
    }

    @Test
    fun `a later call reads the path it got and returns the previous link`() = runTest {
        val repository = repository(routes = arrayOf("/sub/api/logs?page=34" to fixture("logs_page_middle.json")))

        val page = repository.getLogs("/sub/api/logs?page=34").getOrThrow()

        assertEquals(listOf("GET /sub/api/logs?page=34"), paths())
        assertEquals(page.logs.sortedByDescending { it.id }, page.logs)
        assertEquals("01a2122a-1ff3-71db-9e37-d426195a61d5", page.logs.first().id)
        assertEquals("/sub/api/logs?page=33", page.nextPath)
    }

    /** Page 1 has no `previous`: the oldest page ends the list. */
    @Test
    fun `the call for the oldest page returns no next path`() = runTest {
        val repository = repository(routes = arrayOf("/sub/api/logs?page=1" to fixture("logs_page_first.json")))

        val page = repository.getLogs("/sub/api/logs?page=1").getOrThrow()

        assertEquals(listOf("GET /sub/api/logs?page=1"), paths())
        assertEquals(30, page.logs.size)
        assertNull(page.nextPath)
    }

    /** The whole list fits on one page: the server sends no `view`. */
    @Test
    fun `a list with no view is one request and the end`() = runTest {
        val repository = repository(
            routes = arrayOf(
                "/sub/api/logs" to
                    """{"member":[${member("a", "11:00:00")},${member("b", "12:00:00")}],"totalItems":2}"""
            )
        )

        val page = repository.getLogs(null).getOrThrow()

        assertEquals(listOf("GET /sub/api/logs"), paths())
        assertEquals(listOf("b", "a"), page.logs.map { it.id })
        assertNull(page.nextPath)
    }

    @Test
    fun `the prefixed hydra keys are read too`() = runTest {
        val repository = repository(
            routes = arrayOf(
                "/sub/api/logs" to """{"hydra:member":[],"hydra:view":{"hydra:last":"/sub/api/logs?page=2"}}""",
                "/sub/api/logs?page=2" to
                    """{"hydra:member":[${member(
                        "b",
                        "12:00:00"
                    )}],"hydra:view":{"hydra:previous":"/sub/api/logs?page=1"}}"""
            )
        )

        val page = repository.getLogs(null).getOrThrow()

        assertEquals(listOf("b"), page.logs.map { it.id })
        assertEquals("/sub/api/logs?page=1", page.nextPath)
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getLogs(null).exceptionOrNull())
        assertIs<KoillectionError.NotFound>(repository.getLogs("/sub/api/logs?page=2").exceptionOrNull())
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `every call keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/logs" to fixture("logs_page_first.json", prefix = ""),
            "/api/logs?page=35" to fixture("logs_page_last.json", prefix = ""),
            "/api/logs?page=34" to fixture("logs_page_middle.json", prefix = "")
        )

        val first = repository.getLogs(null).getOrThrow()
        repository.getLogs(first.nextPath).getOrThrow()

        assertEquals(listOf("GET /api/logs", "GET /api/logs?page=35", "GET /api/logs?page=34"), paths())
    }

    private fun member(id: String, time: String) =
        """{"id":"$id","type":"create","loggedAt":"2026-11-25T$time+01:00","objectLabel":"$id",""" +
            """"objectClass":"App\\Entity\\Item"}"""
}
