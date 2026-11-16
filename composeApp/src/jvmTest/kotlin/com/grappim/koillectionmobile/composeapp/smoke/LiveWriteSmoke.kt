package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * The local test instance from `docs/local-info.md`. Never the user's real server: it holds the
 * working book catalogue, and this run writes and deletes.
 */
private const val SERVER_URL = "http://127.0.0.1:8290"
private const val USERNAME = "tester"
private const val PASSWORD = "koillection-test"
private const val COLLECTION_TITLE = "Books"

/**
 * Live write smoke run (M3.1, M5.2): in collection `Books` on the local test instance it creates an
 * item, two text fields, an image field, uploads a main image and the field image, sets two
 * existing tags and reads the item back. Then it edits (M5.2): it renames the item, changes a text
 * field's label and value, replaces the main image and the field image, tries a duplicate label
 * (rejected, see the comment there) and deletes the image field. Last it deletes the item and reads it
 * once more to see the 404. After each replace or delete, the old file must give 404. Run it with
 * `./gradlew :composeApp:smokeWriteRun`.
 *
 * If the run fails after the create, it still tries the delete.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    var createdItemId: String? = null
    val exitCode = try {
        runBlocking {
            val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
            println("login: $login")
            check(login == LoginResult.LoggedIn) { "login failed" }

            val collection = koin.get<CollectionsRepository>().getCollections().getOrThrow()
                .single { it.title == COLLECTION_TITLE }
            val tagsRepository = koin.get<TagsRepository>()
            val tags = tagsRepository.getTags().getOrThrow().take(2)
            check(tags.size == 2) { "the test instance has fewer than two tags" }

            val items = koin.get<ItemsRepository>()
            val item = items.createItem(collection.id, "M3.1 smoke ${System.currentTimeMillis()}").getOrThrow()
            createdItemId = item.id
            println("created item: $item")
            println("  text: ${items.createTextDatum(item.id, "Author", "Smoke Author", position = 1).getOrThrow()}")
            println("  text: ${items.createTextDatum(item.id, "Year", "2026", position = 2).getOrThrow()}")
            val imageDatum = items.createImageDatum(item.id, "Page 1", position = 3).getOrThrow()
            println("  image field: $imageDatum")
            val withImage = items.uploadItemImage(item.id, jpeg(Color.ORANGE), "main.jpg", JPEG).getOrThrow()
            println("  main image: $withImage")
            val fieldWithImage = items.uploadDatumImage(imageDatum.id, jpeg(Color.CYAN), "page1.jpg", JPEG).getOrThrow()
            println("  field image: $fieldWithImage")
            tagsRepository.setItemTags(item.id, tags.map { it.id }).getOrThrow()
            println("  tags set: ${tags.map { it.label }}")

            println("read back:")
            println("  item: ${items.getItem(item.id).getOrThrow()}")
            items.getData(item.id).getOrThrow().forEach {
                println("  [${it.position}] ${it.type} ${it.label}: ${it.value ?: it.image ?: "-"}")
            }
            println("  tags: ${tagsRepository.getItemTags(item.id).getOrThrow().map { it.label }}")

            HttpClient().use { files -> edit(items, item.id, files) }

            items.deleteItem(item.id).getOrThrow()
            println("DELETE item ${item.id}: ok")
            createdItemId = null
            val afterDelete = items.getItem(item.id).exceptionOrNull()
            println("GET after delete: $afterDelete")
            check(afterDelete is KoillectionError.NotFound) { "item still readable after the delete" }
        }
        0
    } catch (e: Exception) {
        System.err.println("smoke write run failed: $e")
        1
    } finally {
        createdItemId?.let { id ->
            runCatching { runBlocking { koin.get<ItemsRepository>().deleteItem(id).getOrThrow() } }
                .onFailure { System.err.println("cleanup delete of $id failed: $it") }
        }
        stopKoin()
    }
    exitProcess(exitCode)
}

/** The M5.2 edits on item [itemId] as created above. [files] is a client without the JWT. */
private suspend fun edit(items: ItemsRepository, itemId: String, files: HttpClient) {
    println("edit:")
    val renamed = items.renameItem(itemId, "M5.2 smoke renamed ${System.currentTimeMillis()}").getOrThrow()
    println("  rename: ${renamed.name}")
    check(items.getItem(itemId).getOrThrow().name == renamed.name) { "rename not read back" }

    val data = items.getData(itemId).getOrThrow()
    val author = data.single { it.label == "Author" }
    val year = data.single { it.label == "Year" }
    val page = data.single { it.label == "Page 1" }

    val changed = items.updateDatum(author.id, label = "Writer", value = "Changed Author").getOrThrow()
    println("  field label and value: ${changed.label} = ${changed.value}")
    check(changed.label == "Writer" && changed.value == "Changed Author") { "field change not applied" }
    val valueOnly = items.updateDatum(year.id, value = "2027").getOrThrow()
    println("  field value only: ${valueOnly.label} = ${valueOnly.value}")
    check(valueOnly.label == "Year" && valueOnly.value == "2027") { "value-only change not applied" }

    val duplicate = items.updateDatum(year.id, label = "Writer").exceptionOrNull()
    println("  duplicate label: $duplicate")
    // Koillection 1.8.5 answers this PATCH with an empty 500, not a 422: the validator rejects it,
    // then the error handler fails on a duplicate key (server log, M5.2). A POST gets the 422.
    check(duplicate is KoillectionError.Server && duplicate.status == 500) { "a duplicate label was not rejected" }
    check(items.getData(itemId).getOrThrow().single { it.id == year.id }.label == "Year") { "duplicate label stored" }

    val oldMain = checkNotNull(items.getItem(itemId).getOrThrow().image).url
    expectStatus(files, oldMain, HttpStatusCode.OK)
    val replacedItem = items.uploadItemImage(itemId, jpeg(Color.GREEN), "main2.jpg", JPEG).getOrThrow()
    val newMain = checkNotNull(replacedItem.image).url
    println("  main image replaced: $newMain")
    check(newMain != oldMain) { "main image URL did not change" }
    expectStatus(files, newMain, HttpStatusCode.OK)
    expectStatus(files, oldMain, HttpStatusCode.NotFound)

    val oldPage = checkNotNull(page.image).url
    expectStatus(files, oldPage, HttpStatusCode.OK)
    val replacedPage = items.uploadDatumImage(page.id, jpeg(Color.MAGENTA), "page1b.jpg", JPEG).getOrThrow()
    val newPage = checkNotNull(replacedPage.image).url
    println("  field image replaced: $newPage")
    check(newPage != oldPage) { "field image URL did not change" }
    expectStatus(files, newPage, HttpStatusCode.OK)
    expectStatus(files, oldPage, HttpStatusCode.NotFound)

    items.deleteDatum(page.id).getOrThrow()
    println("  image field deleted")
    expectStatus(files, newPage, HttpStatusCode.NotFound)
    check(items.getData(itemId).getOrThrow().none { it.id == page.id }) { "deleted field still listed" }

    println("read back after edit:")
    println("  item: ${items.getItem(itemId).getOrThrow()}")
    items.getData(itemId).getOrThrow().forEach {
        println("  [${it.position}] ${it.type} ${it.label}: ${it.value ?: it.image ?: "-"}")
    }
}

private suspend fun expectStatus(files: HttpClient, url: String, expected: HttpStatusCode) {
    val status = files.get(url).status
    println("    GET $url: $status")
    check(status == expected) { "expected $expected for $url, got $status" }
}

/** A small solid JPEG, enough for the server's `Assert\Image`. */
private fun jpeg(color: Color): ByteArray {
    val image = BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB)
    image.createGraphics().apply {
        this.color = color
        fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE)
        dispose()
    }
    return ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
}

private const val IMAGE_SIZE = 64
private const val JPEG = "image/jpeg"
