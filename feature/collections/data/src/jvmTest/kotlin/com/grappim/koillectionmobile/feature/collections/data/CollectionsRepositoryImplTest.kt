package com.grappim.koillectionmobile.feature.collections.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.mapper.CollectionMapper
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

/**
 * Bodies are the M1.1 fixtures from the 1.8.5 local test instance. The server URL has a subpath,
 * so each test also checks that the request path keeps it.
 */
class CollectionsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): CollectionsRepositoryImpl {
        val byPath = routes.toMap()
        val session = FakeSessionStorage(serverUrl = "https://koi.example/sub", token = "jwt")
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
        return CollectionsRepositoryImpl(
            httpClient = client,
            baseUrlProvider = baseUrlProvider,
            mapper = CollectionMapper()
        )
    }

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    private val books = Collection(
        id = BOOKS_ID,
        title = "Books",
        parentId = null,
        color = "EEEEEE",
        imageUrl = null
    )

    @Test
    fun `getCollections reads the list page and maps each member`() = runTest {
        val repository = repository("/sub/api/collections" to (HttpStatusCode.OK to fixture("collections_page.json")))

        val result = repository.getCollections().getOrThrow()

        assertEquals(listOf(books), result)
        assertEquals(listOf("/sub/api/collections"), requests.map { it.url.fullPath })
    }

    @Test
    fun `getCollections follows the next link to the last page`() = runTest {
        val repository = repository(
            "/sub/api/collections" to (
                HttpStatusCode.OK to
                    """{"member":[{"id":"a","title":"A"}],"totalItems":2,""" +
                    """"view":{"next":"/sub/api/collections?page=2"}}"""
                ),
            "/sub/api/collections?page=2" to (
                HttpStatusCode.OK to
                    """{"member":[{"id":"b","title":"B","parent":"/sub/api/collections/a"}],"totalItems":2}"""
                )
        )

        val result = repository.getCollections().getOrThrow()

        assertEquals(listOf("a" to null, "b" to "a"), result.map { it.id to it.parentId })
    }

    @Test
    fun `getCollection reads one collection by id`() = runTest {
        val repository = repository(
            "/sub/api/collections/$BOOKS_ID" to (HttpStatusCode.OK to fixture("collection_one.json"))
        )

        assertEquals(books, repository.getCollection(BOOKS_ID).getOrThrow())
    }

    @Test
    fun `an image path resolves against the stored server URL, subpath included`() = runTest {
        val repository = repository(
            "/sub/api/collections/a" to
                (HttpStatusCode.OK to """{"id":"a","title":"A","image":"uploads/user/cover.jpg"}""")
        )

        assertEquals(
            "https://koi.example/sub/uploads/user/cover.jpg",
            repository.getCollection("a").getOrThrow().imageUrl
        )
    }

    @Test
    fun `getChildren reads the children path, and an empty page is an empty list`() = runTest {
        val repository = repository(
            "/sub/api/collections/$BOOKS_ID/children" to
                (HttpStatusCode.OK to fixture("collection_children_page.json"))
        )

        assertEquals(emptyList(), repository.getChildren(BOOKS_ID).getOrThrow())
        assertEquals(listOf("/sub/api/collections/$BOOKS_ID/children"), requests.map { it.url.fullPath })
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        val error = repository.getCollection("missing").exceptionOrNull()

        assertIs<KoillectionError.NotFound>(error)
    }

    private companion object {
        const val BOOKS_ID = "01a1de6f-bfad-78b6-a916-7266d9e3dfff"
    }
}
