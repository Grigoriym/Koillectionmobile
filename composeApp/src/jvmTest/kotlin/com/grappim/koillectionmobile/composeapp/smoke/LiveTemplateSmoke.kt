package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin
import kotlin.system.exitProcess

/**
 * The local test instance from `docs/local-info.md`. Never the user's real server: it holds the
 * working book catalogue, and this run writes and deletes.
 */
private const val SERVER_URL = "http://127.0.0.1:8290"
private const val USERNAME = "tester"
private const val PASSWORD = "koillection-test"

private val CHOICES = listOf("Hardcover", "Paperback", "Ebook")

/**
 * Live template smoke run (M8.2) on the local test instance. Run it with
 * `./gradlew :composeApp:smokeTemplateRun`.
 *
 * It creates a choice list, a template with a `text`, `number`, `checkbox` and `choice-list`
 * field, and a top-level collection. It sets the template as the collection's
 * `itemsDefaultTemplate`, reads `/api/collections/{id}/items_default_template` and clears the
 * default again. It adds an item with a `checkbox`, `choice-list`, `number` and `date` field and
 * prints what the server stores. Then it deletes the template while it is the default again, and
 * prints what happens to its fields and to the collection. Last it deletes everything else.
 *
 * It also sends a `choice-list` field without a choice list, two fields with one name in one
 * template, and a `checkbox` value `true`, and prints each answer.
 *
 * The app has no call for the collection's default template or for a `checkbox` or `choice-list`
 * field yet (M8.6, M8.8), so the run sends those through the app's own `HttpClient`. If the run
 * fails halfway, it still deletes what it created.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    val created = Created()
    val exitCode = try {
        runBlocking { run(koin, created) }
        0
    } catch (e: Exception) {
        System.err.println("smoke template run failed: $e")
        1
    } finally {
        runBlocking { cleanUp(koin, created) }
        stopKoin()
    }
    exitProcess(exitCode)
}

/** What the run made and has not deleted yet. */
private class Created {
    var choiceListId: String? = null
    var templateId: String? = null
    var collectionId: String? = null
}

@Suppress("LongMethod")
private suspend fun run(koin: Koin, created: Created) {
    val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
    println("login: $login")
    check(login == LoginResult.LoggedIn) { "login failed" }

    val templates = koin.get<TemplatesRepository>()
    val collections = koin.get<CollectionsRepository>()
    val items = koin.get<ItemsRepository>()
    val client = koin.get<HttpClient>()
    val stamp = System.currentTimeMillis()
    val countsBefore = counts(koin)
    println("before: $countsBefore")

    // Choice list and template with one field per type.
    val choiceList = templates.createChoiceList("M8.2 smoke $stamp", CHOICES).getOrThrow()
    created.choiceListId = choiceList.id
    println("created choice list: $choiceList")
    check(choiceList.choices == CHOICES) { "the choice list does not keep the choices in order" }

    val template = templates.createTemplate("M8.2 smoke $stamp").getOrThrow()
    created.templateId = template.id
    println("created template: $template")

    // Created out of position order: the read must sort them.
    val checkbox = templates.createField(template.id, "Signed", 3, "checkbox").getOrThrow()
    val text = templates.createField(template.id, "Publisher", 1, "text").getOrThrow()
    val number = templates.createField(template.id, "Pages", 2, "number").getOrThrow()
    val choice = templates.createField(template.id, "Format", 4, "choice-list", choiceList.id).getOrThrow()
    val fields = templates.getFields(template.id).getOrThrow()
    println("fields: ${fields.map { "${it.position} ${it.name} ${it.type} ${it.choiceListId}" }}")
    check(fields.map { it.id } == listOf(text.id, number.id, checkbox.id, choice.id)) {
        "the fields are not in position order"
    }
    check(fields.last().choiceListId == choiceList.id) { "the choice-list field lost its choice list" }

    val noList = templates.createField(template.id, "No list", 5, "choice-list").exceptionOrNull()
    println("choice-list field without a choiceList: $noList")
    check(noList is KoillectionError.Validation) { "a choice-list field without a choice list was not a 422" }

    val sameName = templates.createField(template.id, text.name, 6, "text")
    println("second field named ${text.name} in one template: $sameName")
    println("  fields after it: ${templates.getFields(template.id).getOrThrow().map { it.name }}")

    // The collection's default template: set, read, clear.
    val collection = collections.createCollection("M8.2 smoke $stamp").getOrThrow()
    created.collectionId = collection.id
    println("created collection: ${collection.title} (${collection.id})")

    setDefaultTemplate(client, collection.id, template.id)
    val default = client.get("api/collections/${collection.id}/items_default_template").body<JsonObject>()
    println("GET items_default_template: id ${default.string("id")}, name ${default.string("name")}")
    check(default.string("id") == template.id) { "the default template is not the template" }

    setDefaultTemplate(client, collection.id, null)
    val cleared = runCatching { client.get("api/collections/${collection.id}/items_default_template") }
    println("GET items_default_template after the clear: ${cleared.exceptionOrNull() ?: cleared.getOrNull()?.status}")
    check(defaultTemplateOf(client, collection.id) == null) { "the default template was not cleared" }

    // An item with typed fields: the value the server stores for each type.
    val item = items.createItem(collection.id, "M8.2 smoke item $stamp").getOrThrow()
    println("created item: ${item.name} (${item.id})")
    val sent = listOf(
        datumBody(item.id, "checkbox", "Signed", 1, "1"),
        datumBody(item.id, "checkbox", "Read", 2, "0"),
        datumBody(item.id, "choice-list", "Format", 3, """["Hardcover","Ebook"]""", choiceList.id),
        datumBody(item.id, "number", "Pages", 4, "352"),
        datumBody(item.id, "date", "Bought", 5, "2026-11-17")
    )
    val datumIds = sent.map { body ->
        val answer = client.post("api/data") {
            contentType(ContentTypeLdJson)
            setBody(body)
        }.body<JsonObject>()
        println("POST /api/data ${body.string("type")} value ${body["value"]}: stored ${answer["value"]}")
        check(answer.string("value") == body.string("value")) { "the server changed the ${body.string("type")} value" }
        answer.string("id")
    }
    val choiceDatum = client.get("api/data/${datumIds[2]}").body<JsonObject>()
    println("  choice-list field choiceList: ${choiceDatum["choiceList"]}")
    check(choiceDatum.string("choiceList") == "/api/choice_lists/${choiceList.id}") { "the field lost its choice list" }
    val readBack = items.getData(item.id).getOrThrow().map { "${it.type} ${it.label}=${it.value}" }
    println("item fields read back: $readBack")

    val wrongCheckbox = runCatching {
        client.post("api/data") {
            contentType(ContentTypeLdJson)
            setBody(datumBody(item.id, "checkbox", "Wrong", 6, "true"))
        }
    }.exceptionOrNull()
    println("checkbox value true: $wrongCheckbox")
    check(wrongCheckbox is KoillectionError.Validation) { "a checkbox value true was not a 422" }

    items.deleteItem(item.id).getOrThrow()
    println("deleted item ${item.id}")

    // A template delete while it is a collection's default.
    setDefaultTemplate(client, collection.id, template.id)
    templates.deleteTemplate(template.id).getOrThrow()
    created.templateId = null
    println("deleted template ${template.id}")
    fields.forEach { field ->
        val read = runCatching { client.get("api/fields/${field.id}") }.exceptionOrNull()
        println("  GET field ${field.name} after the template delete: $read")
        check(read is KoillectionError.NotFound) { "field ${field.id} is still there" }
    }
    val defaultAfter = defaultTemplateOf(client, collection.id)
    println("  collection itemsDefaultTemplate after the template delete: $defaultAfter")
    check(defaultAfter == null) { "the collection still points at the deleted template" }

    templates.deleteChoiceList(choiceList.id).getOrThrow()
    created.choiceListId = null
    println("deleted choice list ${choiceList.id}")
    collections.deleteCollection(collection.id).getOrThrow()
    created.collectionId = null
    println("deleted collection ${collection.id}")

    val countsAfter = counts(koin)
    println("after: $countsAfter")
    check(countsAfter == countsBefore) { "the test instance does not end as it started" }
}

/** Templates, choice lists and collections of the user: the run must leave them as it found them. */
private suspend fun counts(koin: Koin): Map<String, Int> {
    val templates = koin.get<TemplatesRepository>()
    return mapOf(
        "templates" to templates.getTemplates().getOrThrow().size,
        "choice lists" to templates.getChoiceLists().getOrThrow().size,
        "collections" to koin.get<CollectionsRepository>().getCollections().getOrThrow().size
    )
}

private suspend fun setDefaultTemplate(client: HttpClient, collectionId: String, templateId: String?) {
    val iri: JsonElement = templateId?.let { JsonPrimitive("/api/templates/$it") } ?: JsonNull
    val answer = client.patch("api/collections/$collectionId") {
        contentType(ContentTypeMergePatchJson)
        setBody(buildJsonObject { put("itemsDefaultTemplate", iri) })
    }.body<JsonObject>()
    println("PATCH collection itemsDefaultTemplate=$iri: ${answer["itemsDefaultTemplate"]}")
}

private suspend fun defaultTemplateOf(client: HttpClient, collectionId: String): String? =
    client.get("api/collections/$collectionId").body<JsonObject>()["itemsDefaultTemplate"]
        ?.takeIf { it != JsonNull }?.jsonPrimitive?.content

/** A `POST /api/data` body with the keys of `NewDatumDto` (`feature:items:dto`) plus `choiceList`. */
private fun datumBody(
    itemId: String,
    type: String,
    label: String,
    position: Int,
    value: String,
    choiceListId: String? = null
): JsonObject = buildJsonObject {
    put("item", "/api/items/$itemId")
    put("type", type)
    put("label", label)
    put("position", position)
    put("value", value)
    choiceListId?.let { put("choiceList", "/api/choice_lists/$it") }
}

private fun JsonObject.string(key: String): String? = get(key)?.takeIf { it != JsonNull }?.jsonPrimitive?.content

/** Deletes what a failed run left. A template delete takes its fields with it. */
private suspend fun cleanUp(koin: Koin, created: Created) {
    val templates = koin.get<TemplatesRepository>()
    created.collectionId?.let { id ->
        koin.get<CollectionsRepository>().deleteCollection(id)
            .onFailure { System.err.println("cleanup delete of collection $id failed: $it") }
    }
    created.templateId?.let { id ->
        templates.deleteTemplate(id).onFailure { System.err.println("cleanup delete of template $id failed: $it") }
    }
    created.choiceListId?.let { id ->
        templates.deleteChoiceList(id).onFailure { System.err.println("cleanup delete of choice list $id failed: $it") }
    }
}
