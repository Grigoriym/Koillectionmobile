package com.grappim.koillectionmobile.feature.templates.data

import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.configureKoillectionClient
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.testing.FakeSessionStorage
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.mapper.TemplateMapper
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The test instance has no templates or choice lists (M8.1), so the bodies are inline, in the
 * shape of the entities' `read` groups (Koillection 1.8.5). The server URL has a subpath, so each
 * test also checks that request paths keep it.
 */
class TemplatesRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val requests = mutableListOf<HttpRequestData>()

    /** "METHOD path" → status and body. Any other request gets a 404. */
    private fun repository(vararg routes: Pair<String, Pair<HttpStatusCode, String>>): TemplatesRepositoryImpl =
        repository("https://koi.example/sub", *routes)

    private fun repository(
        serverUrl: String,
        vararg routes: Pair<String, Pair<HttpStatusCode, String>>
    ): TemplatesRepositoryImpl {
        val byRequest = routes.toMap()
        val session = FakeSessionStorage(serverUrl = serverUrl, token = "jwt")
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                val (status, body) = byRequest["${request.method.value} ${request.url.fullPath}"]
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
        return TemplatesRepositoryImpl(httpClient = client, mapper = TemplateMapper())
    }

    private fun HttpRequestData.line(): String = "${method.value} ${url.fullPath}"

    private fun assertBody(expected: String, request: HttpRequestData) {
        assertEquals(json.parseToJsonElement(expected), json.parseToJsonElement((request.body as TextContent).text))
    }

    // Templates

    @Test
    fun `getTemplates reads every page`() = runTest {
        val repository = repository(
            "GET /sub/api/templates" to (
                HttpStatusCode.OK to
                    """{"member":[$TEMPLATE],"view":{"next":"/sub/api/templates?page=2"}}"""
                ),
            "GET /sub/api/templates?page=2" to (HttpStatusCode.OK to """{"member":[{"id":"t2","name":"Vinyl"}]}""")
        )

        val templates = repository.getTemplates().getOrThrow()

        assertEquals(listOf(Template("t1", "Book"), Template("t2", "Vinyl")), templates)
        assertEquals(listOf("GET /sub/api/templates", "GET /sub/api/templates?page=2"), requests.map { it.line() })
    }

    @Test
    fun `getTemplate reads the template path`() = runTest {
        val repository = repository("GET /sub/api/templates/t1" to (HttpStatusCode.OK to TEMPLATE))

        assertEquals(Template("t1", "Book"), repository.getTemplate("t1").getOrThrow())
    }

    @Test
    fun `createTemplate posts only the name as ld+json`() = runTest {
        val repository = repository("POST /sub/api/templates" to (HttpStatusCode.Created to TEMPLATE))

        val template = repository.createTemplate("Book").getOrThrow()

        val request = requests.single()
        assertEquals("POST /sub/api/templates", request.line())
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertBody("""{"name":"Book"}""", request)
        assertEquals(Template("t1", "Book"), template)
    }

    @Test
    fun `renameTemplate patches only the name as merge-patch+json`() = runTest {
        val repository = repository("PATCH /sub/api/templates/t1" to (HttpStatusCode.OK to TEMPLATE))

        repository.renameTemplate("t1", "Book").getOrThrow()

        val request = requests.single()
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertBody("""{"name":"Book"}""", request)
    }

    @Test
    fun `deleteTemplate sends DELETE to the template path`() = runTest {
        val repository = repository("DELETE /sub/api/templates/t1" to (HttpStatusCode.NoContent to ""))

        repository.deleteTemplate("t1").getOrThrow()

        assertEquals(listOf("DELETE /sub/api/templates/t1"), requests.map { it.line() })
    }

    @Test
    fun `a rejected createTemplate is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository(
            "POST /sub/api/templates" to (
                HttpStatusCode.UnprocessableEntity to
                    """{"title":"An error occurred","detail":"name: This value should not be blank."}"""
                )
        )

        assertIs<KoillectionError.Validation>(repository.createTemplate("").exceptionOrNull())
    }

    // Fields

    @Test
    fun `getFields reads every page of the template's fields and sorts them by position`() = runTest {
        val repository = repository(
            "GET /sub/api/templates/t1/fields" to (
                HttpStatusCode.OK to
                    """{"member":[${field("f3", "Read", 3, "checkbox")},$CHOICE_FIELD],""" +
                    """"view":{"next":"/sub/api/templates/t1/fields?page=2"}}"""
                ),
            "GET /sub/api/templates/t1/fields?page=2" to (
                HttpStatusCode.OK to """{"member":[${field("f1", "Author", 1, "text")}]}"""
                )
        )

        val fields = repository.getFields("t1").getOrThrow()

        assertEquals(listOf("f1", "f2", "f3"), fields.map { it.id })
        assertEquals(
            TemplateField(id = "f2", name = "Genre", position = 2, type = "choice-list", choiceListId = "c1"),
            fields[1]
        )
        assertEquals(null, fields[0].choiceListId)
    }

    @Test
    fun `createField posts name position type and the template IRI as ld+json`() = runTest {
        val repository = repository(
            "POST /sub/api/fields" to (HttpStatusCode.Created to field("f1", "Author", 1, "text"))
        )

        val field = repository.createField(templateId = "t1", name = "Author", position = 1, type = "text")
            .getOrThrow()

        val request = requests.single()
        assertEquals("POST /sub/api/fields", request.line())
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertBody("""{"name":"Author","position":1,"type":"text","template":"/api/templates/t1"}""", request)
        assertEquals("f1", field.id)
    }

    @Test
    fun `createField of a choice-list field sends the choice list IRI`() = runTest {
        val repository = repository("POST /sub/api/fields" to (HttpStatusCode.Created to CHOICE_FIELD))

        repository.createField(
            templateId = "t1",
            name = "Genre",
            position = 2,
            type = "choice-list",
            choiceListId = "c1"
        ).getOrThrow()

        assertBody(
            """{"name":"Genre","position":2,"type":"choice-list","template":"/api/templates/t1",""" +
                """"choiceList":"/api/choice_lists/c1"}""",
            requests.single()
        )
    }

    @Test
    fun `editField patches only the given keys as merge-patch+json`() = runTest {
        val repository = repository(
            "PATCH /sub/api/fields/f1" to (HttpStatusCode.OK to field("f1", "Writer", 4, "text"))
        )

        val field = repository.editField(id = "f1", name = "Writer", position = 4).getOrThrow()

        val request = requests.single()
        assertEquals("PATCH /sub/api/fields/f1", request.line())
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertBody("""{"name":"Writer","position":4}""", request)
        assertEquals("Writer", field.name)
    }

    @Test
    fun `editField to a choice-list type sends the type and the choice list IRI`() = runTest {
        val repository = repository("PATCH /sub/api/fields/f2" to (HttpStatusCode.OK to CHOICE_FIELD))

        repository.editField(id = "f2", type = "choice-list", choiceListId = "c1").getOrThrow()

        assertBody("""{"type":"choice-list","choiceList":"/api/choice_lists/c1"}""", requests.single())
    }

    @Test
    fun `deleteField sends DELETE to the field path`() = runTest {
        val repository = repository("DELETE /sub/api/fields/f1" to (HttpStatusCode.NoContent to ""))

        repository.deleteField("f1").getOrThrow()

        assertEquals(listOf("DELETE /sub/api/fields/f1"), requests.map { it.line() })
    }

    // Choice lists

    @Test
    fun `getChoiceLists reads every page and keeps the order of the choices`() = runTest {
        val repository = repository(
            "GET /sub/api/choice_lists" to (
                HttpStatusCode.OK to
                    """{"member":[$CHOICE_LIST],"view":{"next":"/sub/api/choice_lists?page=2"}}"""
                ),
            "GET /sub/api/choice_lists?page=2" to (
                HttpStatusCode.OK to """{"member":[{"id":"c2","name":"Format","choices":[]}]}"""
                )
        )

        val lists = repository.getChoiceLists().getOrThrow()

        assertEquals(
            listOf(
                ChoiceList("c1", "Genres", listOf("Poetry", "Novel", "Essay")),
                ChoiceList("c2", "Format", emptyList())
            ),
            lists
        )
    }

    @Test
    fun `createChoiceList posts the name and the choices as ld+json`() = runTest {
        val repository = repository("POST /sub/api/choice_lists" to (HttpStatusCode.Created to CHOICE_LIST))

        repository.createChoiceList("Genres", listOf("Poetry", "Novel", "Essay")).getOrThrow()

        val request = requests.single()
        assertEquals("POST /sub/api/choice_lists", request.line())
        assertEquals("application/ld+json", request.body.contentType.toString())
        assertBody("""{"name":"Genres","choices":["Poetry","Novel","Essay"]}""", request)
    }

    @Test
    fun `editChoiceList patches the name and the full choices as merge-patch+json`() = runTest {
        val repository = repository("PATCH /sub/api/choice_lists/c1" to (HttpStatusCode.OK to CHOICE_LIST))

        repository.editChoiceList("c1", "Genres", listOf("Essay", "Poetry")).getOrThrow()

        val request = requests.single()
        assertEquals("PATCH /sub/api/choice_lists/c1", request.line())
        assertEquals("application/merge-patch+json", request.body.contentType.toString())
        assertBody("""{"name":"Genres","choices":["Essay","Poetry"]}""", request)
    }

    @Test
    fun `deleteChoiceList sends DELETE to the choice list path`() = runTest {
        val repository = repository("DELETE /sub/api/choice_lists/c1" to (HttpStatusCode.NoContent to ""))

        repository.deleteChoiceList("c1").getOrThrow()

        assertEquals(listOf("DELETE /sub/api/choice_lists/c1"), requests.map { it.line() })
    }

    @Test
    fun `a failed delete is a failed result that holds the KoillectionError`() = runTest {
        val repository = repository()

        assertIs<KoillectionError.NotFound>(repository.deleteChoiceList("missing").exceptionOrNull())
    }

    /** `CLAUDE.md`, Verification: at a root-path server Ktor's merged path differs from `/sub`. */
    @Test
    fun `every path holds at a root-path server`() = runTest {
        val repository = repository(
            "https://koi.example",
            "GET /api/templates" to (HttpStatusCode.OK to """{"member":[$TEMPLATE]}"""),
            "PATCH /api/templates/t1" to (HttpStatusCode.OK to TEMPLATE),
            "GET /api/templates/t1/fields" to (HttpStatusCode.OK to """{"member":[$CHOICE_FIELD]}"""),
            "POST /api/fields" to (HttpStatusCode.Created to CHOICE_FIELD),
            "DELETE /api/fields/f2" to (HttpStatusCode.NoContent to ""),
            "POST /api/choice_lists" to (HttpStatusCode.Created to CHOICE_LIST)
        )

        repository.getTemplates().getOrThrow()
        repository.renameTemplate("t1", "Book").getOrThrow()
        repository.getFields("t1").getOrThrow()
        repository.createField("t1", "Genre", 2, "choice-list", "c1").getOrThrow()
        repository.deleteField("f2").getOrThrow()
        repository.createChoiceList("Genres", listOf("Poetry")).getOrThrow()

        assertEquals(
            listOf(
                "GET /api/templates",
                "PATCH /api/templates/t1",
                "GET /api/templates/t1/fields",
                "POST /api/fields",
                "DELETE /api/fields/f2",
                "POST /api/choice_lists"
            ),
            requests.map { it.line() }
        )
    }

    private companion object {
        const val TEMPLATE =
            """{"@id":"/api/templates/t1","@type":"Template","id":"t1","name":"Book","owner":"/api/users/u1",""" +
                """"createdAt":"2026-11-17T10:00:00+00:00","updatedAt":null}"""

        const val CHOICE_LIST =
            """{"@id":"/api/choice_lists/c1","@type":"ChoiceList","id":"c1","name":"Genres",""" +
                """"choices":["Poetry","Novel","Essay"],"owner":"/api/users/u1"}"""

        val CHOICE_FIELD = field("f2", "Genre", 2, "choice-list", choiceList = "\"/api/choice_lists/c1\"")

        fun field(id: String, name: String, position: Int, type: String, choiceList: String = "null"): String =
            """{"@id":"/api/fields/$id","@type":"Field","id":"$id","name":"$name","position":$position,""" +
                """"type":"$type","choiceList":$choiceList,"template":"/api/templates/t1","visibility":"public"}"""
    }
}
