package com.grappim.koillectionmobile.feature.tags.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.mapper.TagMapper
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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Bodies are the M1.1 fixtures from the 1.8.5 local test instance. The server URL has a subpath,
 * so each test also checks that request paths keep it.
 */
class TagsRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** Path (with query) → status and body. Any other path gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): TagsRepositoryImpl =
        repository("https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): TagsRepositoryImpl {
        val byPath = routes.toMap()
        val session = FakeSessionStorage(serverUrl = serverUrl, token = "jwt")
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
                baseUrlProvider = object : BaseUrlProvider {
                    override fun getBaseUrl(): String = session.serverUrl
                },
                sessionStorage = session,
                customHeadersStorage = FakeCustomHeadersStorage(),
                logLevel = LogLevel.NONE
            )
        }
        return TagsRepositoryImpl(httpClient = client, mapper = TagMapper())
    }

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    @Test
    fun `getTags reads the tags path and maps every member`() = runTest {
        val repository = repository("/sub/api/tags" to (HttpStatusCode.OK to fixture("tags_page.json")))

        val tags = repository.getTags().getOrThrow()

        assertEquals(30, tags.size)
        assertEquals(listOf("/sub/api/tags"), requests.map { it.url.fullPath })
        assertEquals("Programming", tags.first().label)
    }

    /** The fixture holds one page (30 tags, the server's page size), so page 2 is an inline body. */
    @Test
    fun `getTags follows the next link`() = runTest {
        val repository = repository(
            "/sub/api/tags" to (
                HttpStatusCode.OK to
                    """{"member":[{"id":"a","label":"A"}],"view":{"next":"/sub/api/tags?page=2"}}"""
                ),
            "/sub/api/tags?page=2" to (HttpStatusCode.OK to """{"member":[{"id":"b","label":"B"}]}""")
        )

        assertEquals(listOf("a", "b"), repository.getTags().getOrThrow().map { it.id })
    }

    @Test
    fun `getTagCategories reads the categories path and maps every member`() = runTest {
        val repository = repository(
            "/sub/api/tag_categories" to (HttpStatusCode.OK to fixture("tag_categories.json"))
        )

        val categories = repository.getTagCategories().getOrThrow()

        assertEquals(listOf("/sub/api/tag_categories"), requests.map { it.url.fullPath })
        assertEquals(4, categories.size)
        assertEquals("Subject", categories.first().label)
    }

    @Test
    fun `getItemTags reads the item's tags path`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID/tags" to (HttpStatusCode.OK to fixture("item_tags.json")))

        val tags = repository.getItemTags(ITEM_ID).getOrThrow()

        assertEquals(listOf("/sub/api/items/$ITEM_ID/tags"), requests.map { it.url.fullPath })
        assertEquals(listOf("Poetry", "German", "Fraktur"), tags.map { it.label })
    }

    @Test
    fun `a server error is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.getItemTags("missing").exceptionOrNull())
    }

    @Test
    fun `setItemTags patches the item with the full list of tag IRIs as merge-patch+json`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID" to (HttpStatusCode.OK to fixture("item_one.json")))

        repository.setItemTags(ITEM_ID, listOf("tag-1", "tag-2")).getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/sub/api/items/$ITEM_ID", request.url.fullPath)
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement("""{"tags":["/api/tags/tag-1","/api/tags/tag-2"]}"""),
            json.parseToJsonElement((request.body as TextContent).text)
        )
    }

    @Test
    fun `setItemTags with an empty list sends an empty tags array`() = runTest {
        val repository = repository("/sub/api/items/$ITEM_ID" to (HttpStatusCode.OK to fixture("item_one.json")))

        repository.setItemTags(ITEM_ID, emptyList()).getOrThrow()

        assertEquals(
            json.parseToJsonElement("""{"tags":[]}"""),
            json.parseToJsonElement((requests.single().body as TextContent).text)
        )
    }

    @Test
    fun `createTag posts only the label as ld+json and maps the answer`() = runTest {
        val repository = repository("/sub/api/tags" to (HttpStatusCode.Created to NEW_TAG))

        val tag = repository.createTag("Sci-fi").getOrThrow()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/sub/api/tags", request.url.fullPath)
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertEquals(
            json.parseToJsonElement("""{"label":"Sci-fi"}"""),
            json.parseToJsonElement((request.body as TextContent).text)
        )
        assertEquals(Tag(id = "new-tag", label = "Sci-fi", description = null, categoryId = null), tag)
    }

    @Test
    fun `a rejected createTag is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "/sub/api/tags" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"label: This value should not be blank."}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.createTag("").exceptionOrNull())
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `createTag keeps its path at a root-path server`() = runTest {
        val repository = repository("https://koi.example", "/api/tags" to (HttpStatusCode.Created to NEW_TAG))

        repository.createTag("Sci-fi").getOrThrow()

        assertEquals(listOf("POST /api/tags"), requests.map { "${it.method.value} ${it.url.fullPath}" })
    }

    private companion object {
        /** A trimmed `POST /api/tags` answer, in the shape of `tags_page.json`'s members. */
        const val NEW_TAG =
            """{"@id":"/api/tags/new-tag","@type":"Tag","id":"new-tag","label":"Sci-fi","description":null,""" +
                """"category":null,"visibility":"public","seenCounter":0}"""

        const val ITEM_ID = "01a1de6f-bff4-7eb7-8d9f-973b4ac86da8"
    }
}
