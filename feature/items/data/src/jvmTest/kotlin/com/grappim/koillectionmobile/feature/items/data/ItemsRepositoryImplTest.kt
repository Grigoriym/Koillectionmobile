package com.grappim.koillectionmobile.feature.items.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.Credentials
import com.grappim.koillectionmobile.feature.items.mapper.ItemMapper
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
import io.ktor.http.encodedPath
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
        return repository { request ->
            val (status, body) = byPath[request.url.fullPath]
                ?: (HttpStatusCode.NotFound to """{"title":"An error occurred","detail":"Not Found"}""")
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/ld+json"))
        }
    }

    private fun repository(
        session: FakeSessionStorage = FakeSessionStorage(serverUrl = "https://koi.example/sub", token = "jwt"),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): ItemsRepositoryImpl {
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

    /**
     * A page of `GET /api/items` as the 1.8.5 test instance sends it (keys cut to the ones the app
     * reads). [next] is the `view.next` link, or null on the last page.
     */
    private fun allItemsPage(names: List<String>, next: String?): String {
        val members = names.joinToString(",") { name ->
            """{"id":"id-$name","name":"$name","quantity":1,"collection":"/api/collections/c-$name","image":null}"""
        }
        val view = next?.let { ""","view":{"@id":"/api/items?page=1","next":"$it"}""" }.orEmpty()
        return """{"member":[$members],"totalItems":5$view}"""
    }

    @Test
    fun `getAllItems reads api items and every next page`() = runTest {
        val repository = repository(
            "/sub/api/items" to (HttpStatusCode.OK to allItemsPage(listOf("a", "b"), next = "/sub/api/items?page=2")),
            "/sub/api/items?page=2" to
                (HttpStatusCode.OK to allItemsPage(listOf("c", "d"), next = "/sub/api/items?page=3")),
            "/sub/api/items?page=3" to (HttpStatusCode.OK to allItemsPage(listOf("e"), next = null))
        )

        val items = repository.getAllItems().getOrThrow()

        assertEquals(listOf("a", "b", "c", "d", "e"), items.map { it.name })
        assertEquals("c-c", items[2].collectionId)
        assertEquals(
            listOf("GET /sub/api/items", "GET /sub/api/items?page=2", "GET /sub/api/items?page=3"),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `getAllItems keeps its path at a root-path server`() = runTest {
        val repository = repository(FakeSessionStorage(serverUrl = "https://koi.example", token = "jwt")) { request ->
            val body = when (request.url.fullPath) {
                "/api/items" -> allItemsPage(listOf("a"), next = "/api/items?page=2")
                "/api/items?page=2" -> allItemsPage(listOf("b"), next = null)
                else -> null
            }
            respond(
                body ?: """{"title":"An error occurred","detail":"Not Found"}""",
                if (body == null) HttpStatusCode.NotFound else HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/ld+json")
            )
        }

        assertEquals(listOf("a", "b"), repository.getAllItems().getOrThrow().map { it.name })
        assertEquals(listOf("/api/items", "/api/items?page=2"), requests.map { it.url.fullPath })
    }

    @Test
    fun `a failed page fails getAllItems with the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/items" to (HttpStatusCode.OK to allItemsPage(listOf("a"), next = "/sub/api/items?page=2"))
        )

        assertIs<KoillectionError.NotFound>(repository.getAllItems().exceptionOrNull())
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

    @Test
    fun `uploadItemImage posts the bytes as multipart part file`() = runTest {
        val repository =
            repository("/sub/api/items/$ITEM_ID/image" to (HttpStatusCode.Created to fixture("item_one.json")))

        val item = repository.uploadItemImage(ITEM_ID, JPEG, "title.jpg", "image/jpeg").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/items/$ITEM_ID/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "file", fileName = "title.jpg", contentType = "image/jpeg", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals(ITEM_ID, item.id)
    }

    @Test
    fun `uploadDatumImage posts the bytes as multipart part fileImage`() = runTest {
        val repository = repository("/sub/api/data/datum-2/image" to (HttpStatusCode.Created to DATUM_IMAGE_UPLOADED))

        val datum = repository.uploadDatumImage("datum-2", JPEG, "p2.webp", "image/webp").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/data/datum-2/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "fileImage", fileName = "p2.webp", contentType = "image/webp", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals("https://koi.example/sub/uploads/u/page.jpg", datum.image?.url)
    }

    /** M1.3: `bearerAuth` sends the same request object again after a 401 and a new login. */
    @Test
    fun `an upload repeated after a 401 sends the complete multipart body again`() = runTest {
        val session = FakeSessionStorage(
            serverUrl = "https://koi.example/sub",
            credentials = Credentials(username = "tester", password = "hunter2"),
            token = "old-jwt"
        )
        val repository = repository(session) { request ->
            val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                request.url.encodedPath.endsWith("/api/authentication_token") ->
                    respond("""{"token":"new-jwt"}""", HttpStatusCode.OK, jsonHeaders)

                request.headers[HttpHeaders.Authorization] == "Bearer new-jwt" ->
                    respond(fixture("item_one.json"), HttpStatusCode.Created, jsonHeaders)

                else -> respond(
                    """{"code":401,"message":"Expired JWT Token"}""",
                    HttpStatusCode.Unauthorized,
                    jsonHeaders
                )
            }
        }

        repository.uploadItemImage(ITEM_ID, JPEG, "title.jpg", "image/jpeg").getOrThrow()

        val uploads = requests.filter { it.url.encodedPath.endsWith("/image") }
        assertEquals(listOf("Bearer old-jwt", "Bearer new-jwt"), uploads.map { it.headers[HttpHeaders.Authorization] })
        val expected =
            listOf(Part(name = "file", fileName = "title.jpg", contentType = "image/jpeg", bytes = JPEG.toList()))
        assertEquals(expected, parts(uploads[0]))
        assertEquals(expected, parts(uploads[1]))
    }

    @Test
    fun `a rejected upload is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/items/$ITEM_ID/image" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"file: This file is not a valid image."}"""
                )
        )

        assertIs<KoillectionError.Validation>(
            repository.uploadItemImage(ITEM_ID, JPEG, "title.jpg", "image/jpeg").exceptionOrNull()
        )
    }

    @Test
    fun `renameItem patches only the name as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID" to (HttpStatusCode.OK to fixture("item_one.json")))

        val item = repository.renameItem(ITEM_ID, "Auswahl").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/items/$ITEM_ID", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"name":"Auswahl"}"""), body(request))
        assertEquals(ITEM_ID, item.id)
    }

    @Test
    fun `updateDatum with a label and a value patches both as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/data/datum-1" to (HttpStatusCode.OK to DATUM_TEXT))

        val datum = repository.updateDatum("datum-1", label = "Author", value = "Theodor Echtermeyer").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/data/datum-1", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement("""{"label":"Author","value":"Theodor Echtermeyer"}"""),
            body(request)
        )
        assertEquals("datum-1", datum.id)
    }

    @Test
    fun `updateDatum with only a label leaves the value key out`() = runTest {
        val repository = repository("/sub/api/data/datum-2" to (HttpStatusCode.OK to DATUM_IMAGE))

        repository.updateDatum("datum-2", label = "Preface").getOrThrow()

        assertEquals(json.parseToJsonElement("""{"label":"Preface"}"""), body(requests.single()))
    }

    @Test
    fun `updateDatum with only a value leaves the label key out`() = runTest {
        val repository = repository("/sub/api/data/datum-1" to (HttpStatusCode.OK to DATUM_TEXT))

        repository.updateDatum("datum-1", value = "").getOrThrow()

        assertEquals(json.parseToJsonElement("""{"value":""}"""), body(requests.single()))
    }

    @Test
    fun `a rejected updateDatum is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/data/datum-1" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"item: error.datum.label_already_used"}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.updateDatum("datum-1", label = "Title").exceptionOrNull())
    }

    @Test
    fun `deleteDatum sends DELETE to the field's path`() = runTest {
        val repository = repository("/sub/api/data/datum-1" to (HttpStatusCode.NoContent to ""))

        repository.deleteDatum("datum-1").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/data/datum-1", request.url.fullPath)
    }

    @Test
    fun `deleteItem sends DELETE to the item's path`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID" to (HttpStatusCode.NoContent to ""))

        repository.deleteItem(ITEM_ID).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/items/$ITEM_ID", request.url.fullPath)
    }

    @Test
    fun `a failed delete is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.deleteItem("missing").exceptionOrNull())
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `the edit and delete calls keep their paths at a root-path server`() = runTest {
        val repository = repository(FakeSessionStorage(serverUrl = "https://koi.example", token = "jwt")) { request ->
            val body = when (request.url.encodedPath) {
                "/api/items/$ITEM_ID" -> fixture("item_one.json")
                "/api/data/datum-1" -> DATUM_TEXT
                else -> """{"title":"An error occurred","detail":"Not Found"}"""
            }
            val status = if (body.startsWith("""{"title"""")) HttpStatusCode.NotFound else HttpStatusCode.OK
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/ld+json"))
        }

        repository.renameItem(ITEM_ID, "Auswahl").getOrThrow()
        repository.updateDatum("datum-1", label = "Author").getOrThrow()
        repository.deleteDatum("datum-1").getOrThrow()
        repository.deleteItem(ITEM_ID).getOrThrow()

        assertEquals(
            listOf(
                "PATCH /api/items/$ITEM_ID",
                "PATCH /api/data/datum-1",
                "DELETE /api/data/datum-1",
                "DELETE /api/items/$ITEM_ID"
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
        /** The server's answer to `POST /api/data`: the new field in the `datum:read` group. */
        const val DATUM_TEXT =
            """{"id":"datum-1","item":"/api/items/01a1de6f-bff4-7eb7-8d9f-973b4ac86da8","type":"text",""" +
                """"label":"Author","value":"Theodor Echtermeyer","position":1,"image":null}"""
        const val DATUM_IMAGE =
            """{"id":"datum-2","item":"/api/items/01a1de6f-bff4-7eb7-8d9f-973b4ac86da8","type":"image",""" +
                """"label":"Preface","value":null,"position":2,"image":null}"""

        /** The server's answer to `POST /api/data/{id}/image`: the field with its stored image. */
        const val DATUM_IMAGE_UPLOADED =
            """{"id":"datum-2","item":"/api/items/01a1de6f-bff4-7eb7-8d9f-973b4ac86da8","type":"image",""" +
                """"label":"Preface","value":null,"position":2,"image":"uploads/u/page.jpg"}"""

        /** A JPEG start marker, a CR LF, a zero and bytes above 0x7F: not valid UTF-8 text. */
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x0D, 0x0A, 0x00, 0x80.toByte(), 0x7F)

        const val BOOKS_ID = "01a1de6f-bfad-78b6-a916-7266d9e3dfff"
        const val ITEM_ID = "01a1de6f-bff4-7eb7-8d9f-973b4ac86da8"
    }
}
