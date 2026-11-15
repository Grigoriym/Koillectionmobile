package com.grappim.koillectionmobile.core.api

import com.grappim.kit.testing.FakeCustomHeadersStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Bodies are the M1.1 fixtures from the 1.8.5 local test instance. `jvmTest`, not `commonTest`:
 * only the JVM reads a file here, and Gradle runs the tests with the module as working directory.
 */
class HydraCollectionTest {

    @Serializable
    private data class IdDto(val id: String)

    private val json = Json { ignoreUnknownKeys = true }

    private fun fixture(name: String): String = File("../../fixtures/koillection-1.8.5/$name").readText()

    private fun decode(body: String): HydraCollectionDto<IdDto> = json.decodeFromString(body)

    @Test
    fun `a first page reads its members, the total and the next link`() {
        val page = decode(fixture("collection_items_page_1.json"))

        assertEquals(30, page.member.size)
        assertEquals("01a1de6f-bff4-7eb7-8d9f-973b4ac86da8", page.member.first().id)
        assertEquals(445, page.totalItems)
        assertEquals("/api/collections/01a1de6f-bfad-78b6-a916-7266d9e3dfff/items?page=2", page.nextPage)
    }

    @Test
    fun `the last page has a view but no next link`() {
        val page = decode(fixture("collection_items_page_last.json"))

        assertEquals(25, page.member.size)
        assertEquals(445, page.totalItems)
        assertNull(page.nextPage)
    }

    @Test
    fun `a list on one page has no view and no next link`() {
        val page = decode(fixture("tags_page.json"))

        assertEquals(30, page.member.size)
        assertEquals(30, page.totalItems)
        assertNull(page.view)
        assertNull(page.nextPage)
    }

    @Test
    fun `an empty list reads as no members`() {
        val page = decode(fixture("collection_children_page.json"))

        assertEquals(emptyList(), page.member)
        assertEquals(0, page.totalItems)
    }

    @Test
    fun `older servers' hydra-prefixed keys read the same`() {
        val page = decode(
            """{"hydra:member":[{"id":"a"}],"hydra:totalItems":31,""" +
                """"hydra:view":{"hydra:next":"/api/tags?page=2"}}"""
        )

        assertEquals(listOf(IdDto("a")), page.member)
        assertEquals(31, page.totalItems)
        assertEquals("/api/tags?page=2", page.nextPage)
    }

    @Test
    fun `getAllHydraPages follows next links to the last page`() = runTest {
        val bodies = mapOf(
            "/api/collections/01a1de6f-bfad-78b6-a916-7266d9e3dfff/items" to fixture("collection_items_page_1.json"),
            // The fixture's next link points at page 2; page 2 here is the captured last page.
            "/api/collections/01a1de6f-bfad-78b6-a916-7266d9e3dfff/items?page=2" to
                fixture("collection_items_page_last.json")
        )
        val requested = mutableListOf<String>()
        val client = client("https://koi.example/") { pathAndQuery ->
            requested += pathAndQuery
            bodies.getValue(pathAndQuery)
        }

        val items = client.getAllHydraPages<IdDto>("api/collections/01a1de6f-bfad-78b6-a916-7266d9e3dfff/items")

        assertEquals(bodies.keys.toList(), requested)
        assertEquals(55, items.size)
    }

    @Test
    fun `getAllHydraPages makes one request for a list without a view`() = runTest {
        val requested = mutableListOf<String>()
        val client = client("https://koi.example/") { pathAndQuery ->
            requested += pathAndQuery
            fixture("tags_page.json")
        }

        val tags = client.getAllHydraPages<IdDto>("api/tags")

        assertEquals(listOf("/api/tags"), requested)
        assertEquals(30, tags.size)
    }

    @Test
    fun `a next link under a sub-path base URL keeps the host and its own path`() = runTest {
        val requested = mutableListOf<String>()
        val client = client("https://koi.example/sub") { pathAndQuery ->
            requested += pathAndQuery
            if (pathAndQuery == "/sub/api/tags") {
                """{"member":[{"id":"a"}],"view":{"next":"/sub/api/tags?page=2"}}"""
            } else {
                """{"member":[{"id":"b"}],"view":{}}"""
            }
        }

        val tags = client.getAllHydraPages<IdDto>("api/tags")

        assertEquals(listOf("/sub/api/tags", "/sub/api/tags?page=2"), requested)
        assertEquals(listOf(IdDto("a"), IdDto("b")), tags)
    }

    /** [body] maps a request's path and query to the ld+json body to answer with. */
    private fun client(baseUrl: String, body: (String) -> String): HttpClient = HttpClient(
        MockEngine { request ->
            val url = request.url
            val pathAndQuery = url.encodedPath + if (url.encodedQuery.isEmpty()) "" else "?${url.encodedQuery}"
            assertEquals("koi.example", url.host)
            respond(body(pathAndQuery), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/ld+json"))
        }
    ) {
        configureKoillectionClient(
            json = json,
            baseUrlProvider = object : BaseUrlProvider {
                override fun getBaseUrl(): String = baseUrl
            },
            sessionStorage = FakeSessionStorage(),
            customHeadersStorage = FakeCustomHeadersStorage(emptyList()),
            logLevel = LogLevel.NONE
        )
    }
}
