package com.grappim.koillectionmobile.feature.collections.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.mapper.CollectionMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
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
 * Bodies are the M1.1 fixtures from the 1.8.5 local test instance. The server URL has a subpath,
 * so each test also checks that the request path keeps it.
 */
class CollectionsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): CollectionsRepositoryImpl =
        repository(serverUrl = "https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): CollectionsRepositoryImpl {
        val byPath = routes.toMap()
        return repository(serverUrl) { request ->
            val (status, body) = byPath[request.url.fullPath]
                ?: (HttpStatusCode.NotFound to """{"title":"An error occurred","detail":"Not Found"}""")
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/ld+json"))
        }
    }

    private fun repository(
        serverUrl: String,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): CollectionsRepositoryImpl {
        val session = FakeSessionStorage(serverUrl = serverUrl, token = "jwt")
        val baseUrlProvider = object : BaseUrlProvider {
            override fun getBaseUrl(): String = session.serverUrl
        }
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
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

    @Test
    fun `createCollection without a parent posts only the title as ld+json`() = runTest {
        val repository = repository("/sub/api/collections" to (HttpStatusCode.Created to NEW_TOP))

        val collection = repository.createCollection("Comics").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/collections", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"title":"Comics"}"""), body(request))
        assertEquals(Collection("new", "Comics", null, "A1B2C3", null), collection)
    }

    @Test
    fun `createCollection with a parent posts the parent IRI`() = runTest {
        val repository = repository("/sub/api/collections" to (HttpStatusCode.Created to NEW_CHILD))

        val collection = repository.createCollection("Novels", parentId = BOOKS_ID).getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"title":"Novels","parent":"/api/collections/$BOOKS_ID"}"""),
            body(requests.single())
        )
        assertEquals(BOOKS_ID, collection.parentId)
    }

    @Test
    fun `a rejected create is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/collections" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"title: This value should not be blank."}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.createCollection("").exceptionOrNull())
    }

    @Test
    fun `renameCollection patches only the title as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/collections/new" to (HttpStatusCode.OK to NEW_TOP))

        repository.renameCollection("new", "Comics").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/collections/new", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"title":"Comics"}"""), body(request))
    }

    @Test
    fun `deleteCollectionImage patches only deleteImage true as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/collections/new" to (HttpStatusCode.OK to NEW_TOP))

        val collection = repository.deleteCollectionImage("new").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/collections/new", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"deleteImage":true}"""), body(request))
        assertEquals(null, collection.imageUrl)
    }

    @Test
    fun `uploadCollectionImage posts the bytes as multipart part file`() = runTest {
        val repository = repository("/sub/api/collections/new/image" to (HttpStatusCode.Created to NEW_WITH_IMAGE))

        val collection = repository.uploadCollectionImage("new", JPEG, "cover.jpg", "image/jpeg").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/collections/new/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "file", fileName = "cover.jpg", contentType = "image/jpeg", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals("https://koi.example/sub/uploads/u/cover.jpg", collection.imageUrl)
    }

    @Test
    fun `deleteCollection sends DELETE to the collection's path`() = runTest {
        val repository = repository("/sub/api/collections/new" to (HttpStatusCode.NoContent to ""))

        repository.deleteCollection("new").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/collections/new", request.url.fullPath)
    }

    @Test
    fun `a failed delete is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.deleteCollection("missing").exceptionOrNull())
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `the write calls keep their paths at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/collections" to (HttpStatusCode.Created to NEW_TOP),
            "/api/collections/new" to (HttpStatusCode.OK to NEW_TOP),
            "/api/collections/new/image" to (HttpStatusCode.Created to NEW_WITH_IMAGE)
        )

        repository.createCollection("Comics").getOrThrow()
        repository.renameCollection("new", "Comics").getOrThrow()
        repository.uploadCollectionImage("new", JPEG, "cover.jpg", "image/jpeg").getOrThrow()
        repository.deleteCollectionImage("new").getOrThrow()
        repository.deleteCollection("new").getOrThrow()

        assertEquals(
            listOf(
                "POST /api/collections",
                "PATCH /api/collections/new",
                "POST /api/collections/new/image",
                "PATCH /api/collections/new",
                "DELETE /api/collections/new"
            ),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    /** One `multipart/form-data` part; [bytes] as a list so that `equals` compares the content. */
    private data class Part(val name: String, val fileName: String, val contentType: String, val bytes: List<Byte>)

    /**
     * Parses the body as RFC 7578 multipart by hand, so the test does not trust Ktor's own
     * reader. ISO-8859-1 maps each byte to one char and back, so the binary part survives.
     */
    private suspend fun parts(request: HttpRequestData): List<Part> {
        val contentType = request.body.contentType!!
        assertEquals("multipart/form-data", contentType.withoutParameters().toString())
        val boundary = contentType.parameter("boundary")!!
        val text = String(request.body.toByteArray(), Charsets.ISO_8859_1)
        val segments = text.split("--$boundary")
        assertEquals("--\r\n", segments.last())
        return segments.drop(1).dropLast(1).map { segment ->
            val (head, content) = segment.removePrefix("\r\n").removeSuffix("\r\n").split("\r\n\r\n", limit = 2)
            val headers = head.split("\r\n").associate { line ->
                line.substringBefore(":").lowercase() to line.substringAfter(":").trim()
            }
            val disposition = headers.getValue("content-disposition")
            Part(
                name = Regex("""\bname=([^;]+)""").find(disposition)!!.groupValues[1].removeSurrounding("\""),
                fileName = Regex("""\bfilename=([^;]+)""").find(disposition)!!.groupValues[1].removeSurrounding("\""),
                contentType = headers.getValue("content-type"),
                bytes = content.toByteArray(Charsets.ISO_8859_1).toList()
            )
        }
    }

    private fun body(request: HttpRequestData): JsonElement =
        json.parseToJsonElement((request.body as TextContent).text)

    private companion object {
        const val BOOKS_ID = "01a1de6f-bfad-78b6-a916-7266d9e3dfff"

        /** The server's answers to the write calls, in the `collection:read` group. */
        const val NEW_TOP = """{"id":"new","title":"Comics","color":"A1B2C3","image":null}"""
        const val NEW_CHILD =
            """{"id":"child","title":"Novels","parent":"/api/collections/$BOOKS_ID","color":"A1B2C3","image":null}"""
        const val NEW_WITH_IMAGE = """{"id":"new","title":"Comics","color":"A1B2C3","image":"uploads/u/cover.jpg"}"""

        /** A JPEG start marker, a CR LF, a zero and bytes above 0x7F: not valid UTF-8 text. */
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x0D, 0x0A, 0x00, 0x80.toByte(), 0x7F)
    }
}
