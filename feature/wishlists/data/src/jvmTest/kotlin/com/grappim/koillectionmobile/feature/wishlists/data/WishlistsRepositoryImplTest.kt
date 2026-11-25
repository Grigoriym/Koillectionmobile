package com.grappim.koillectionmobile.feature.wishlists.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.mapper.WishlistMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The test copy of the catalogue has no wishlists, so the bodies are written from the
 * `wishlist:read` and `wish:read` groups of Koillection 1.8.5 (`Wishlist.php`, `Wish.php`). The
 * server URL has a subpath, so each test also checks that the request path keeps it.
 */
class WishlistsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): WishlistsRepositoryImpl =
        repository(serverUrl = "https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): WishlistsRepositoryImpl {
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
        return WishlistsRepositoryImpl(
            httpClient = client,
            baseUrlProvider = baseUrlProvider,
            mapper = WishlistMapper()
        )
    }

    // Wishlists

    @Test
    fun `getWishlists follows the next link and maps each member`() = runTest {
        val repository = repository(
            "/sub/api/wishlists" to (
                HttpStatusCode.OK to
                    """{"member":[$TOP],"totalItems":2,"view":{"next":"/sub/api/wishlists?page=2"}}"""
                ),
            "/sub/api/wishlists?page=2" to (HttpStatusCode.OK to """{"member":[$CHILD],"totalItems":2}""")
        )

        val result = repository.getWishlists().getOrThrow()

        assertEquals(listOf(topWishlist, childWishlist), result)
        assertEquals(
            listOf("/sub/api/wishlists", "/sub/api/wishlists?page=2"),
            requests.map { it.url.fullPath }
        )
    }

    @Test
    fun `getWishlist reads one wishlist and resolves its image against the server URL`() = runTest {
        val repository = repository("/sub/api/wishlists/top" to (HttpStatusCode.OK to TOP_WITH_IMAGE))

        assertEquals(
            topWishlist.copy(imageUrl = "https://koi.example/sub/uploads/u/list.jpg"),
            repository.getWishlist("top").getOrThrow()
        )
    }

    @Test
    fun `getChildren reads the children path`() = runTest {
        val repository = repository(
            "/sub/api/wishlists/top/children" to (HttpStatusCode.OK to """{"member":[$CHILD],"totalItems":1}""")
        )

        assertEquals(listOf(childWishlist), repository.getChildren("top").getOrThrow())
        assertEquals(listOf("/sub/api/wishlists/top/children"), requests.map { it.url.fullPath })
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getWishlist("missing").exceptionOrNull())
        assertIs<KoillectionError.NotFound>(repository.getWish("missing").exceptionOrNull())
    }

    @Test
    fun `createWishlist without a parent posts only the name as ld+json`() = runTest {
        val repository = repository("/sub/api/wishlists" to (HttpStatusCode.Created to TOP))

        val wishlist = repository.createWishlist("Games").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/wishlists", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"name":"Games"}"""), body(request))
        assertEquals(topWishlist, wishlist)
    }

    @Test
    fun `createWishlist with a parent posts the parent IRI`() = runTest {
        val repository = repository("/sub/api/wishlists" to (HttpStatusCode.Created to CHILD))

        val wishlist = repository.createWishlist("Board games", parentId = "top").getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"name":"Board games","parent":"/api/wishlists/top"}"""),
            body(requests.single())
        )
        assertEquals("top", wishlist.parentId)
    }

    @Test
    fun `a rejected create is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/wishlists" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"name: This value should not be blank."}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.createWishlist("").exceptionOrNull())
    }

    @Test
    fun `renameWishlist patches only the name as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/wishlists/top" to (HttpStatusCode.OK to TOP))

        repository.renameWishlist("top", "Games").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/wishlists/top", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"name":"Games"}"""), body(request))
    }

    @Test
    fun `deleteWishlistImage patches only deleteImage true as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/wishlists/top" to (HttpStatusCode.OK to TOP))

        val wishlist = repository.deleteWishlistImage("top").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/wishlists/top", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"deleteImage":true}"""), body(request))
        assertEquals(null, wishlist.imageUrl)
    }

    @Test
    fun `uploadWishlistImage posts the bytes as multipart part file`() = runTest {
        val repository = repository("/sub/api/wishlists/top/image" to (HttpStatusCode.Created to TOP_WITH_IMAGE))

        val wishlist = repository.uploadWishlistImage("top", JPEG, "list.jpg", "image/jpeg").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/wishlists/top/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "file", fileName = "list.jpg", contentType = "image/jpeg", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals("https://koi.example/sub/uploads/u/list.jpg", wishlist.imageUrl)
    }

    @Test
    fun `deleteWishlist sends DELETE to the wishlist's path`() = runTest {
        val repository = repository("/sub/api/wishlists/top" to (HttpStatusCode.NoContent to ""))

        repository.deleteWishlist("top").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/wishlists/top", request.url.fullPath)
    }

    // Wishes

    @Test
    fun `getWishes follows the next link of the wishlist's wishes path`() = runTest {
        val repository = repository(
            "/sub/api/wishlists/child/wishes" to (
                HttpStatusCode.OK to
                    """{"member":[$WISH],"totalItems":2,"view":{"next":"/sub/api/wishlists/child/wishes?page=2"}}"""
                ),
            "/sub/api/wishlists/child/wishes?page=2" to (
                HttpStatusCode.OK to
                    """{"member":[{"id":"w2","name":"Dice","wishlist":"/api/wishlists/child"}],"totalItems":2}"""
                )
        )

        val result = repository.getWishes("child").getOrThrow()

        assertEquals(listOf(wish, bareWish("w2", "Dice")), result)
    }

    @Test
    fun `getWish reads one wish by id and maps every field`() = runTest {
        val repository = repository("/sub/api/wishes/w1" to (HttpStatusCode.OK to WISH))

        assertEquals(wish, repository.getWish("w1").getOrThrow())
        assertEquals(listOf("/sub/api/wishes/w1"), requests.map { it.url.fullPath })
    }

    @Test
    fun `createWish posts every field and the wishlist IRI as ld+json`() = runTest {
        val repository = repository("/sub/api/wishes" to (HttpStatusCode.Created to WISH))

        val created = repository.createWish(
            wishlistId = "child",
            name = "Catan",
            url = "https://shop.example/catan",
            price = "39.90",
            currency = "EUR",
            comment = "Base game"
        ).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/wishes", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement(
                """{"name":"Catan","wishlist":"/api/wishlists/child","url":"https://shop.example/catan",""" +
                    """"price":"39.90","currency":"EUR","comment":"Base game"}"""
            ),
            body(request)
        )
        assertEquals(wish, created)
    }

    @Test
    fun `createWish leaves the null fields out of the body`() = runTest {
        val repository = repository("/sub/api/wishes" to (HttpStatusCode.Created to WISH))

        repository.createWish(wishlistId = "child", name = "Catan").getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"name":"Catan","wishlist":"/api/wishlists/child"}"""),
            body(requests.single())
        )
    }

    @Test
    fun `editWish patches only the changed keys as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/wishes/w1" to (HttpStatusCode.OK to WISH))

        repository.editWish("w1", mapOf(WishField.PRICE to "35", WishField.COMMENT to "Sale")).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/wishes/w1", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"price":"35","comment":"Sale"}"""), body(request))
    }

    @Test
    fun `editWish names each field by its server key`() = runTest {
        val repository = repository("/sub/api/wishes/w1" to (HttpStatusCode.OK to WISH))

        repository.editWish(
            "w1",
            mapOf(
                WishField.NAME to "n",
                WishField.URL to "u",
                WishField.PRICE to "p",
                WishField.CURRENCY to "c",
                WishField.COMMENT to "m"
            )
        ).getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"name":"n","url":"u","price":"p","currency":"c","comment":"m"}"""),
            body(requests.single())
        )
    }

    @Test
    fun `editWish sends a null value as an explicit null`() = runTest {
        val repository = repository("/sub/api/wishes/w1" to (HttpStatusCode.OK to WISH))

        repository.editWish("w1", mapOf(WishField.URL to null)).getOrThrow()

        assertEquals(json.parseToJsonElement("""{"url":null}"""), body(requests.single()))
    }

    @Test
    fun `uploadWishImage posts the bytes as multipart part file`() = runTest {
        val repository = repository("/sub/api/wishes/w1/image" to (HttpStatusCode.Created to WISH))

        val uploaded = repository.uploadWishImage("w1", JPEG, "catan.jpg", "image/jpeg").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/wishes/w1/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "file", fileName = "catan.jpg", contentType = "image/jpeg", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals("https://koi.example/sub/uploads/u/catan.jpg", uploaded.imageUrl)
    }

    @Test
    fun `deleteWish sends DELETE to the wish's path`() = runTest {
        val repository = repository("/sub/api/wishes/w1" to (HttpStatusCode.NoContent to ""))

        repository.deleteWish("w1").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/wishes/w1", request.url.fullPath)
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `every call keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/wishlists" to (HttpStatusCode.Created to TOP),
            "/api/wishlists/top" to (HttpStatusCode.OK to TOP),
            "/api/wishlists/top/children" to (HttpStatusCode.OK to """{"member":[]}"""),
            "/api/wishlists/top/image" to (HttpStatusCode.Created to TOP),
            "/api/wishlists/top/wishes" to (HttpStatusCode.OK to """{"member":[]}"""),
            "/api/wishes" to (HttpStatusCode.Created to WISH),
            "/api/wishes/w1" to (HttpStatusCode.OK to WISH),
            "/api/wishes/w1/image" to (HttpStatusCode.Created to WISH)
        )

        repository.createWishlist("Games").getOrThrow()
        repository.getWishlist("top").getOrThrow()
        repository.getChildren("top").getOrThrow()
        repository.renameWishlist("top", "Games").getOrThrow()
        repository.uploadWishlistImage("top", JPEG, "list.jpg", "image/jpeg").getOrThrow()
        repository.deleteWishlistImage("top").getOrThrow()
        repository.getWishes("top").getOrThrow()
        repository.createWish("top", "Catan").getOrThrow()
        repository.getWish("w1").getOrThrow()
        repository.editWish("w1", mapOf(WishField.NAME to "Catan")).getOrThrow()
        repository.uploadWishImage("w1", JPEG, "catan.jpg", "image/jpeg").getOrThrow()
        repository.deleteWish("w1").getOrThrow()
        repository.deleteWishlist("top").getOrThrow()

        assertEquals(
            listOf(
                "POST /api/wishlists",
                "GET /api/wishlists/top",
                "GET /api/wishlists/top/children",
                "PATCH /api/wishlists/top",
                "POST /api/wishlists/top/image",
                "PATCH /api/wishlists/top",
                "GET /api/wishlists/top/wishes",
                "POST /api/wishes",
                "GET /api/wishes/w1",
                "PATCH /api/wishes/w1",
                "POST /api/wishes/w1/image",
                "DELETE /api/wishes/w1",
                "DELETE /api/wishlists/top"
            ),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    /** The routes key on the path only, so the list read gets its own root-path case. */
    @Test
    fun `getWishlists keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/wishlists" to (HttpStatusCode.OK to """{"member":[$TOP]}""")
        )

        repository.getWishlists().getOrThrow()

        assertEquals(listOf("GET /api/wishlists"), requests.map { "${it.method.value} ${it.url.fullPath}" })
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

    private val topWishlist = Wishlist(id = "top", name = "Games", parentId = null, color = "A1B2C3", imageUrl = null)
    private val childWishlist =
        Wishlist(id = "child", name = "Board games", parentId = "top", color = "D4E5F6", imageUrl = null)

    private val wish = Wish(
        id = "w1",
        name = "Catan",
        wishlistId = "child",
        url = "https://shop.example/catan",
        price = "39.90",
        currency = "EUR",
        comment = "Base game",
        imageUrl = "https://koi.example/sub/uploads/u/catan.jpg",
        thumbnailUrl = "https://koi.example/sub/uploads/u/catan_small.jpg"
    )

    private fun bareWish(id: String, name: String) = Wish(
        id = id,
        name = name,
        wishlistId = "child",
        url = null,
        price = null,
        currency = null,
        comment = null,
        imageUrl = null,
        thumbnailUrl = null
    )

    private companion object {
        /** Bodies in the `wishlist:read` group. */
        const val TOP = """{"id":"top","name":"Games","color":"A1B2C3","image":null,"visibility":"public"}"""
        const val TOP_WITH_IMAGE = """{"id":"top","name":"Games","color":"A1B2C3","image":"uploads/u/list.jpg"}"""
        const val CHILD =
            """{"id":"child","name":"Board games","parent":"/api/wishlists/top","color":"D4E5F6","image":null}"""

        /** A body in the `wish:read` group, with every field the app reads. */
        const val WISH = """{"id":"w1","name":"Catan","wishlist":"/api/wishlists/child",""" +
            """"url":"https://shop.example/catan","price":"39.90","currency":"EUR","comment":"Base game",""" +
            """"image":"uploads/u/catan.jpg","imageSmallThumbnail":"uploads/u/catan_small.jpg",""" +
            """"visibility":"public"}"""

        /** A JPEG start marker, a CR LF, a zero and bytes above 0x7F: not valid UTF-8 text. */
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x0D, 0x0A, 0x00, 0x80.toByte(), 0x7F)
    }
}
