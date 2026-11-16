package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
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

/**
 * Live collection smoke run (M6.2) on the local test instance. It creates a top-level collection
 * and a child, renames the child, uploads an image to the child, replaces it and removes it. After
 * the replace and the remove, the old file must give 404. It adds an item to the child, then
 * deletes the parent: the child and the item must give 404 after it. Last it sends a create with
 * an empty title, which the server must reject. Run it with
 * `./gradlew :composeApp:smokeCollectionRun`.
 *
 * If the run fails after the first create, it still tries to delete the parent.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    var createdParentId: String? = null
    val exitCode = try {
        runBlocking {
            val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
            println("login: $login")
            check(login == LoginResult.LoggedIn) { "login failed" }

            val collections = koin.get<CollectionsRepository>()
            val items = koin.get<ItemsRepository>()
            val stamp = System.currentTimeMillis()

            val parent = collections.createCollection("M6.2 smoke parent $stamp").getOrThrow()
            createdParentId = parent.id
            println("created parent: $parent")
            check(parent.parentId == null) { "the parent is not top level" }

            val child = collections.createCollection("M6.2 smoke child $stamp", parent.id).getOrThrow()
            println("created child: $child")
            check(child.parentId == parent.id) { "the child has parent ${child.parentId}" }
            check(collections.getChildren(parent.id).getOrThrow().map { it.id } == listOf(child.id)) {
                "the parent does not list the child"
            }

            val renamed = collections.renameCollection(child.id, "M6.2 smoke child renamed $stamp").getOrThrow()
            println("rename: ${renamed.title}")
            check(collections.getCollection(child.id).getOrThrow().title == renamed.title) { "rename not read back" }
            check(renamed.parentId == parent.id) { "the rename changed the parent" }

            HttpClient().use { files ->
                val first = collections.uploadCollectionImage(child.id, jpeg(Color.ORANGE), "c1.jpg", JPEG)
                    .getOrThrow().imageUrl
                    .let { checkNotNull(it) { "no image after the upload" } }
                println("image uploaded: $first")
                expectStatus(files, first, HttpStatusCode.OK)

                val second = collections.uploadCollectionImage(child.id, jpeg(Color.GREEN), "c2.jpg", JPEG)
                    .getOrThrow().imageUrl
                    .let { checkNotNull(it) { "no image after the replace" } }
                println("image replaced: $second")
                check(second != first) { "image URL did not change" }
                expectStatus(files, second, HttpStatusCode.OK)
                expectStatus(files, first, HttpStatusCode.NotFound)

                val removed = collections.deleteCollectionImage(child.id).getOrThrow()
                println("image removed: imageUrl = ${removed.imageUrl}")
                check(removed.imageUrl == null) { "image still set in the PATCH answer" }
                check(collections.getCollection(child.id).getOrThrow().imageUrl == null) { "image still set on read" }
                expectStatus(files, second, HttpStatusCode.NotFound)
            }

            val item = items.createItem(child.id, "M6.2 smoke item $stamp").getOrThrow()
            println("item in child: ${item.id}")

            collections.deleteCollection(parent.id).getOrThrow()
            println("DELETE parent ${parent.id}: ok")
            createdParentId = null
            expectNotFound("parent", collections.getCollection(parent.id).exceptionOrNull())
            expectNotFound("child", collections.getCollection(child.id).exceptionOrNull())
            expectNotFound("item", items.getItem(item.id).exceptionOrNull())

            val empty = collections.createCollection("").exceptionOrNull()
            println("create with empty title: $empty")
            check(empty is KoillectionError.Validation) { "an empty title was not rejected with 400 or 422" }
        }
        0
    } catch (e: Exception) {
        System.err.println("smoke collection run failed: $e")
        1
    } finally {
        createdParentId?.let { id ->
            runCatching { runBlocking { koin.get<CollectionsRepository>().deleteCollection(id).getOrThrow() } }
                .onFailure { System.err.println("cleanup delete of collection $id failed: $it") }
        }
        stopKoin()
    }
    exitProcess(exitCode)
}

private fun expectNotFound(what: String, error: Throwable?) {
    println("  GET $what after the delete: $error")
    check(error is KoillectionError.NotFound) { "$what still readable after the delete" }
}

private suspend fun expectStatus(files: HttpClient, url: String, expected: HttpStatusCode) {
    val status = files.get(url).status
    println("  GET $url: $status")
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
