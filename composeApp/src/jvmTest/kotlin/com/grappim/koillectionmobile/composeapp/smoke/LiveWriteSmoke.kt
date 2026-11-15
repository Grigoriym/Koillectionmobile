package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
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
 * Live write smoke run (M3.1): in collection `Books` on the local test instance it creates an item,
 * two text fields, an image field, uploads a main image and the field image, sets two existing
 * tags, reads the item back, deletes it, and reads it once more to see the 404. Run it with
 * `./gradlew :composeApp:smokeWriteRun`.
 *
 * The repositories have no delete (the app does not need one yet), so the delete goes through the
 * app's authenticated `HttpClient`. If the run fails after the create, it still tries the delete.
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

            deleteItem(koin.get(), item.id)
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
            runCatching { runBlocking { deleteItem(koin.get(), id) } }
                .onFailure { System.err.println("cleanup delete of $id failed: $it") }
        }
        stopKoin()
    }
    exitProcess(exitCode)
}

private suspend fun deleteItem(client: HttpClient, id: String) {
    val response = client.delete("api/items/$id")
    println("DELETE api/items/$id: ${response.status}")
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
