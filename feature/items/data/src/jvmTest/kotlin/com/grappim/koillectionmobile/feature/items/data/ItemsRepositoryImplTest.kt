package com.grappim.koillectionmobile.feature.items.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.items.mapper.ItemMapper
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Bodies are the M1.1 fixtures from the 1.8.5 local test instance. The server URL has a subpath
 * and no trailing slash, so each test also checks that request paths and image URLs keep it.
 */
class ItemsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): ItemsRepositoryImpl {
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
        return ItemsRepositoryImpl(httpClient = client, baseUrlProvider = baseUrlProvider, mapper = ItemMapper())
    }

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    /**
     * The fixture's `next` links have no `/sub` prefix (the test instance runs at the root), so
     * the routes serve them at the path the client requests. Page 1 links to page 2; the route
     * serves the last page (15) there, which has no `next` link.
     */
    @Test
    fun `getItems follows the next link to the last page and maps every member`() = runTest {
        val repository = repository(
            "/sub/api/collections/$BOOKS_ID/items" to (HttpStatusCode.OK to fixture("collection_items_page_1.json")),
            "/api/collections/$BOOKS_ID/items?page=2" to
                (HttpStatusCode.OK to fixture("collection_items_page_last.json"))
        )

        val items = repository.getItems(BOOKS_ID).getOrThrow()

        assertEquals(30 + 25, items.size)
        assertEquals(
            listOf("/sub/api/collections/$BOOKS_ID/items", "/api/collections/$BOOKS_ID/items?page=2"),
            requests.map { it.url.fullPath }
        )
        assertEquals("Auswahl Deutscher Gedichte für höhere Schulen", items.first().name)
        assertEquals(setOf(BOOKS_ID), items.map { it.collectionId }.toSet())
    }

    @Test
    fun `getItem reads one item and resolves its image against the server URL`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID" to (HttpStatusCode.OK to fixture("item_one.json")))

        val item = repository.getItem(ITEM_ID).getOrThrow()

        assertEquals(ITEM_ID, item.id)
        assertEquals(
            "https://koi.example/sub/uploads/01a1de6f-03c6-7568-a2b7-9d7a173d6776/Cq7YNpThMJzPpVrKinW5.jpg",
            item.image?.url
        )
    }

    @Test
    fun `getData reads the item's data path and maps all fields`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID/data" to (HttpStatusCode.OK to fixture("item_data.json")))

        val data = repository.getData(ITEM_ID).getOrThrow()

        assertEquals(23, data.size)
        assertEquals(listOf("/sub/api/items/$ITEM_ID/data"), requests.map { it.url.fullPath })
        assertEquals(11, data.count { it.type == "text" })
        assertEquals(
            12,
            data.count {
                it.type == "image" &&
                    it.image?.url?.startsWith("https://koi.example/sub/uploads/") == true
            }
        )
    }

    @Test
    fun `getData sorts by position, and a field without a position comes last`() = runTest {
        val repository = repository(
            "/sub/api/items/$ITEM_ID/data" to (
                HttpStatusCode.OK to
                    """{"member":[{"id":"none","type":"text"},{"id":"two","type":"text","position":2},""" +
                    """{"id":"one","type":"text","position":1}]}"""
                )
        )

        assertEquals(listOf("one", "two", "none"), repository.getData(ITEM_ID).getOrThrow().map { it.id })
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getItem("missing").exceptionOrNull())
    }

    @Test
    fun `createItem posts the name and the collection IRI as ld+json`() = runTest {
        val repository = repository("/sub/api/items" to (HttpStatusCode.Created to fixture("item_one.json")))

        val item = repository.createItem(BOOKS_ID, "Auswahl").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/items", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement("""{"name":"Auswahl","collection":"/api/collections/$BOOKS_ID"}"""),
            body(request)
        )
        assertEquals(ITEM_ID, item.id)
    }

    @Test
    fun `createTextDatum posts a text field with its value and position`() = runTest {
        val repository = repository("/sub/api/data" to (HttpStatusCode.Created to DATUM_TEXT))

        val datum = repository.createTextDatum(ITEM_ID, "Author", "Theodor Echtermeyer", 1).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/data", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement(
                """{"item":"/api/items/$ITEM_ID","type":"text","label":"Author","position":1,""" +
                    """"value":"Theodor Echtermeyer"}"""
            ),
            body(request)
        )
        assertEquals("datum-1", datum.id)
    }

    @Test
    fun `createImageDatum posts an image field without a value key`() = runTest {
        val repository = repository("/sub/api/data" to (HttpStatusCode.Created to DATUM_IMAGE))

        val datum = repository.createImageDatum(ITEM_ID, "Preface", 2).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/data", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement("""{"item":"/api/items/$ITEM_ID","type":"image","label":"Preface","position":2}"""),
            body(request)
        )
        assertEquals("image", datum.type)
        assertEquals(null, datum.image)
    }

    @Test
    fun `a rejected create is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/data" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"label: This value should not be blank."}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.createTextDatum(ITEM_ID, "", "x", 1).exceptionOrNull())
    }

    private fun body(request: HttpRequestData): JsonElement =
        json.parseToJsonElement((request.body as TextContent).text)

    private companion object {
        /** The server's answer to `POST /api/data`: the new field in the `datum:read` group. */
        const val DATUM_TEXT =
            """{"id":"datum-1","item":"/api/items/01a1de6f-bff4-7eb7-8d9f-973b4ac86da8","type":"text",""" +
                """"label":"Author","value":"Theodor Echtermeyer","position":1,"image":null}"""
        const val DATUM_IMAGE =
            """{"id":"datum-2","item":"/api/items/01a1de6f-bff4-7eb7-8d9f-973b4ac86da8","type":"image",""" +
                """"label":"Preface","value":null,"position":2,"image":null}"""
        const val BOOKS_ID = "01a1de6f-bfad-78b6-a916-7266d9e3dfff"
        const val ITEM_ID = "01a1de6f-bff4-7eb7-8d9f-973b4ac86da8"
    }
}
