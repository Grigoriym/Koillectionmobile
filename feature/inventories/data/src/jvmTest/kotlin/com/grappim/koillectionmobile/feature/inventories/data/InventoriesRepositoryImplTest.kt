package com.grappim.koillectionmobile.feature.inventories.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.inventories.mapper.InventoryMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Bodies are the M12.1 fixtures from the 1.8.5 local test instance. The server URL has a subpath,
 * so each test also checks that the request path keeps it. The mapper's own tests check the tree.
 */
class InventoriesRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): InventoriesRepositoryImpl =
        repository(serverUrl = "https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): InventoriesRepositoryImpl {
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
        return InventoriesRepositoryImpl(httpClient = client, mapper = InventoryMapper())
    }

    @Test
    fun `getInventories reads the list page`() = runTest {
        val repository = repository("/sub/api/inventories" to (HttpStatusCode.OK to fixture("inventories_page.json")))

        val result = repository.getInventories().getOrThrow()

        assertEquals(listOf("Comics shelf check", "Board games check"), result.map { it.name })
        assertEquals(listOf("GET /sub/api/inventories"), requests.map { "${it.method.value} ${it.url.fullPath}" })
    }

    @Test
    fun `getInventories follows the next link`() = runTest {
        val repository = repository(
            "/sub/api/inventories" to (
                HttpStatusCode.OK to
                    """{"member":[${member("a")}],"totalItems":2,"view":{"next":"/sub/api/inventories?page=2"}}"""
                ),
            "/sub/api/inventories?page=2" to (HttpStatusCode.OK to """{"member":[${member("b")}],"totalItems":2}""")
        )

        assertEquals(listOf("a", "b"), repository.getInventories().getOrThrow().map { it.id })
        assertEquals(
            listOf("GET /sub/api/inventories", "GET /sub/api/inventories?page=2"),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    @Test
    fun `getInventory reads the inventory's path`() = runTest {
        val repository =
            repository("/sub/api/inventories/$COMICS_ID" to (HttpStatusCode.OK to fixture("inventory_one.json")))

        val inventory = repository.getInventory(COMICS_ID).getOrThrow()

        assertEquals("Comics shelf check", inventory.name)
        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/sub/api/inventories/$COMICS_ID", request.url.fullPath)
    }

    @Test
    fun `deleteInventory sends DELETE to the inventory's path`() = runTest {
        val repository = repository("/sub/api/inventories/$COMICS_ID" to (HttpStatusCode.NoContent to ""))

        repository.deleteInventory(COMICS_ID).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/inventories/$COMICS_ID", request.url.fullPath)
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getInventories().exceptionOrNull())
        assertIs<KoillectionError.NotFound>(repository.getInventory("missing").exceptionOrNull())
        assertIs<KoillectionError.NotFound>(repository.deleteInventory("missing").exceptionOrNull())
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `every call keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/inventories" to (HttpStatusCode.OK to fixture("inventories_page.json")),
            "/api/inventories/$COMICS_ID" to (HttpStatusCode.OK to fixture("inventory_one.json"))
        )

        repository.getInventories().getOrThrow()
        repository.getInventory(COMICS_ID).getOrThrow()
        repository.deleteInventory(COMICS_ID).getOrThrow()

        assertEquals(
            listOf(
                "GET /api/inventories",
                "GET /api/inventories/$COMICS_ID",
                "DELETE /api/inventories/$COMICS_ID"
            ),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    private fun member(id: String) =
        """{"id":"$id","name":"$id","content":[],"createdAt":"2026-11-25T10:19:26+01:00"}"""

    private companion object {
        const val COMICS_ID = "01a21200-b8e9-70a7-86c3-017c367f3432"
    }
}
