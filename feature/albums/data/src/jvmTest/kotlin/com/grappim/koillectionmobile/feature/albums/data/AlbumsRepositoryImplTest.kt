package com.grappim.koillectionmobile.feature.albums.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.feature.albums.mapper.AlbumMapper
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
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The test copy of the catalogue has no albums and no photos, so the bodies are written from the
 * `album:read` and `photo:read` groups of Koillection 1.8.5 (`Album.php`, `Photo.php`). The server
 * URL has a subpath, so each test also checks that the request path keeps it.
 */
class AlbumsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): AlbumsRepositoryImpl =
        repository(serverUrl = "https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): AlbumsRepositoryImpl {
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
        return AlbumsRepositoryImpl(
            httpClient = client,
            baseUrlProvider = baseUrlProvider,
            mapper = AlbumMapper()
        )
    }

    // Albums

    @Test
    fun `getAlbums follows the next link and maps each member`() = runTest {
        val repository = repository(
            "/sub/api/albums" to (
                HttpStatusCode.OK to
                    """{"member":[$TOP],"totalItems":2,"view":{"next":"/sub/api/albums?page=2"}}"""
                ),
            "/sub/api/albums?page=2" to (HttpStatusCode.OK to """{"member":[$CHILD],"totalItems":2}""")
        )

        val result = repository.getAlbums().getOrThrow()

        assertEquals(listOf(topAlbum, childAlbum), result)
        assertEquals(listOf("/sub/api/albums", "/sub/api/albums?page=2"), requests.map { it.url.fullPath })
    }

    @Test
    fun `getAlbum reads one album and resolves its image against the server URL`() = runTest {
        val repository = repository("/sub/api/albums/top" to (HttpStatusCode.OK to TOP_WITH_IMAGE))

        assertEquals(
            topAlbum.copy(imageUrl = "https://koi.example/sub/uploads/u/album.jpg"),
            repository.getAlbum("top").getOrThrow()
        )
    }

    @Test
    fun `getChildren reads the children path`() = runTest {
        val repository = repository(
            "/sub/api/albums/top/children" to (HttpStatusCode.OK to """{"member":[$CHILD],"totalItems":1}""")
        )

        assertEquals(listOf(childAlbum), repository.getChildren("top").getOrThrow())
        assertEquals(listOf("/sub/api/albums/top/children"), requests.map { it.url.fullPath })
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getAlbum("missing").exceptionOrNull())
        assertIs<KoillectionError.NotFound>(repository.getPhoto("missing").exceptionOrNull())
    }

    @Test
    fun `createAlbum without a parent posts only the title as ld+json`() = runTest {
        val repository = repository("/sub/api/albums" to (HttpStatusCode.Created to TOP))

        val album = repository.createAlbum("Holidays").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/albums", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"title":"Holidays"}"""), body(request))
        assertEquals(topAlbum, album)
    }

    @Test
    fun `createAlbum with a parent posts the parent IRI`() = runTest {
        val repository = repository("/sub/api/albums" to (HttpStatusCode.Created to CHILD))

        val album = repository.createAlbum("Summer", parentId = "top").getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"title":"Summer","parent":"/api/albums/top"}"""),
            body(requests.single())
        )
        assertEquals("top", album.parentId)
    }

    @Test
    fun `a rejected create is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/albums" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"title: This value should not be blank."}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.createAlbum("").exceptionOrNull())
    }

    @Test
    fun `renameAlbum patches only the title as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/albums/top" to (HttpStatusCode.OK to TOP))

        repository.renameAlbum("top", "Holidays").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/albums/top", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"title":"Holidays"}"""), body(request))
    }

    @Test
    fun `deleteAlbumImage patches only deleteImage true as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/albums/top" to (HttpStatusCode.OK to TOP))

        val album = repository.deleteAlbumImage("top").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/albums/top", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"deleteImage":true}"""), body(request))
        assertEquals(null, album.imageUrl)
    }

    @Test
    fun `uploadAlbumImage posts the bytes as multipart part file`() = runTest {
        val repository = repository("/sub/api/albums/top/image" to (HttpStatusCode.Created to TOP_WITH_IMAGE))

        val album = repository.uploadAlbumImage("top", JPEG, "album.jpg", "image/jpeg").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/albums/top/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "file", fileName = "album.jpg", contentType = "image/jpeg", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals("https://koi.example/sub/uploads/u/album.jpg", album.imageUrl)
    }

    @Test
    fun `deleteAlbum sends DELETE to the album's path`() = runTest {
        val repository = repository("/sub/api/albums/top" to (HttpStatusCode.NoContent to ""))

        repository.deleteAlbum("top").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/albums/top", request.url.fullPath)
    }

    // Photos

    @Test
    fun `getPhotos follows the next link of the album's photos path`() = runTest {
        val repository = repository(
            "/sub/api/albums/child/photos" to (
                HttpStatusCode.OK to
                    """{"member":[$PHOTO],"totalItems":2,"view":{"next":"/sub/api/albums/child/photos?page=2"}}"""
                ),
            "/sub/api/albums/child/photos?page=2" to (
                HttpStatusCode.OK to
                    """{"member":[{"id":"p2","title":"Dune","album":"/api/albums/child"}],"totalItems":2}"""
                )
        )

        val result = repository.getPhotos("child").getOrThrow()

        assertEquals(listOf(photo, barePhoto("p2", "Dune")), result)
        assertEquals(
            listOf("/sub/api/albums/child/photos", "/sub/api/albums/child/photos?page=2"),
            requests.map { it.url.fullPath }
        )
    }

    @Test
    fun `getPhoto reads one photo by id and maps every field`() = runTest {
        val repository = repository("/sub/api/photos/p1" to (HttpStatusCode.OK to PHOTO))

        assertEquals(photo, repository.getPhoto("p1").getOrThrow())
        assertEquals(listOf("/sub/api/photos/p1"), requests.map { it.url.fullPath })
    }

    @Test
    fun `createPhoto posts every field and the album IRI as ld+json`() = runTest {
        val repository = repository("/sub/api/photos" to (HttpStatusCode.Created to PHOTO))

        val created = repository.createPhoto(
            albumId = "child",
            title = "Beach",
            comment = "Low tide",
            place = "Brest"
        ).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/photos", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement(
                """{"title":"Beach","album":"/api/albums/child","comment":"Low tide","place":"Brest"}"""
            ),
            body(request)
        )
        assertEquals(photo, created)
    }

    @Test
    fun `createPhoto leaves the null fields out of the body`() = runTest {
        val repository = repository("/sub/api/photos" to (HttpStatusCode.Created to PHOTO))

        repository.createPhoto(albumId = "child", title = "Beach").getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"title":"Beach","album":"/api/albums/child"}"""),
            body(requests.single())
        )
    }

    @Test
    fun `editPhoto patches only the changed keys as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/photos/p1" to (HttpStatusCode.OK to PHOTO))

        repository.editPhoto("p1", mapOf(PhotoField.PLACE to "Quimper")).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/photos/p1", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(json.parseToJsonElement("""{"place":"Quimper"}"""), body(request))
    }

    @Test
    fun `editPhoto names each field by its server key`() = runTest {
        val repository = repository("/sub/api/photos/p1" to (HttpStatusCode.OK to PHOTO))

        repository.editPhoto(
            "p1",
            mapOf(PhotoField.TITLE to "t", PhotoField.COMMENT to "c", PhotoField.PLACE to "p")
        ).getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"title":"t","comment":"c","place":"p"}"""),
            body(requests.single())
        )
    }

    @Test
    fun `editPhoto sends a null value as an explicit null`() = runTest {
        val repository = repository("/sub/api/photos/p1" to (HttpStatusCode.OK to PHOTO))

        repository.editPhoto("p1", mapOf(PhotoField.COMMENT to null)).getOrThrow()

        assertEquals(json.parseToJsonElement("""{"comment":null}"""), body(requests.single()))
    }

    @Test
    fun `uploadPhotoImage posts the bytes as multipart part file`() = runTest {
        val repository = repository("/sub/api/photos/p1/image" to (HttpStatusCode.Created to PHOTO))

        val uploaded = repository.uploadPhotoImage("p1", JPEG, "beach.jpg", "image/jpeg").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/photos/p1/image", request.url.fullPath)
        assertEquals(
            listOf(Part(name = "file", fileName = "beach.jpg", contentType = "image/jpeg", bytes = JPEG.toList())),
            parts(request)
        )
        assertEquals("https://koi.example/sub/uploads/u/beach.jpg", uploaded.imageUrl)
    }

    @Test
    fun `deletePhoto sends DELETE to the photo's path`() = runTest {
        val repository = repository("/sub/api/photos/p1" to (HttpStatusCode.NoContent to ""))

        repository.deletePhoto("p1").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/sub/api/photos/p1", request.url.fullPath)
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `every call keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/albums" to (HttpStatusCode.Created to TOP),
            "/api/albums/top" to (HttpStatusCode.OK to TOP),
            "/api/albums/top/children" to (HttpStatusCode.OK to """{"member":[]}"""),
            "/api/albums/top/image" to (HttpStatusCode.Created to TOP),
            "/api/albums/top/photos" to (HttpStatusCode.OK to """{"member":[]}"""),
            "/api/photos" to (HttpStatusCode.Created to PHOTO),
            "/api/photos/p1" to (HttpStatusCode.OK to PHOTO),
            "/api/photos/p1/image" to (HttpStatusCode.Created to PHOTO)
        )

        repository.createAlbum("Holidays").getOrThrow()
        repository.getAlbum("top").getOrThrow()
        repository.getChildren("top").getOrThrow()
        repository.renameAlbum("top", "Holidays").getOrThrow()
        repository.uploadAlbumImage("top", JPEG, "album.jpg", "image/jpeg").getOrThrow()
        repository.deleteAlbumImage("top").getOrThrow()
        repository.getPhotos("top").getOrThrow()
        repository.createPhoto("top", "Beach").getOrThrow()
        repository.getPhoto("p1").getOrThrow()
        repository.editPhoto("p1", mapOf(PhotoField.TITLE to "Beach")).getOrThrow()
        repository.uploadPhotoImage("p1", JPEG, "beach.jpg", "image/jpeg").getOrThrow()
        repository.deletePhoto("p1").getOrThrow()
        repository.deleteAlbum("top").getOrThrow()

        assertEquals(
            listOf(
                "POST /api/albums",
                "GET /api/albums/top",
                "GET /api/albums/top/children",
                "PATCH /api/albums/top",
                "POST /api/albums/top/image",
                "PATCH /api/albums/top",
                "GET /api/albums/top/photos",
                "POST /api/photos",
                "GET /api/photos/p1",
                "PATCH /api/photos/p1",
                "POST /api/photos/p1/image",
                "DELETE /api/photos/p1",
                "DELETE /api/albums/top"
            ),
            requests.map { "${it.method.value} ${it.url.fullPath}" }
        )
    }

    /** The routes key on the path only, so the list read gets its own root-path case. */
    @Test
    fun `getAlbums keeps its path at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "/api/albums" to (HttpStatusCode.OK to """{"member":[$TOP]}""")
        )

        repository.getAlbums().getOrThrow()

        assertEquals(listOf("GET /api/albums"), requests.map { "${it.method.value} ${it.url.fullPath}" })
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

    private val topAlbum = Album(id = "top", title = "Holidays", parentId = null, color = "A1B2C3", imageUrl = null)
    private val childAlbum =
        Album(id = "child", title = "Summer", parentId = "top", color = "D4E5F6", imageUrl = null)

    private val photo = Photo(
        id = "p1",
        title = "Beach",
        albumId = "child",
        comment = "Low tide",
        place = "Brest",
        imageUrl = "https://koi.example/sub/uploads/u/beach.jpg",
        thumbnailUrl = "https://koi.example/sub/uploads/u/beach_small.jpg",
        takenAt = LocalDate(2026, 7, 14)
    )

    private fun barePhoto(id: String, title: String) = Photo(
        id = id,
        title = title,
        albumId = "child",
        comment = null,
        place = null,
        imageUrl = null,
        thumbnailUrl = null,
        takenAt = null
    )

    private companion object {
        /** Bodies in the `album:read` group. */
        const val TOP = """{"id":"top","title":"Holidays","color":"A1B2C3","image":null,"visibility":"public"}"""
        const val TOP_WITH_IMAGE = """{"id":"top","title":"Holidays","color":"A1B2C3","image":"uploads/u/album.jpg"}"""
        const val CHILD =
            """{"id":"child","title":"Summer","parent":"/api/albums/top","color":"D4E5F6","image":null}"""

        /** A body in the `photo:read` group, with every field the app reads. */
        const val PHOTO = """{"id":"p1","title":"Beach","album":"/api/albums/child",""" +
            """"comment":"Low tide","place":"Brest",""" +
            """"image":"uploads/u/beach.jpg","imageSmallThumbnail":"uploads/u/beach_small.jpg",""" +
            """"takenAt":"2026-07-14T10:00:00+02:00","visibility":"public"}"""

        /** A JPEG start marker, a CR LF, a zero and bytes above 0x7F: not valid UTF-8 text. */
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x0D, 0x0A, 0x00, 0x80.toByte(), 0x7F)
    }
}
