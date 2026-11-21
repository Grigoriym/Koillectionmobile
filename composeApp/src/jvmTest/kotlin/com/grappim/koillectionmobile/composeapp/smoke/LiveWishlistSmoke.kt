package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.core.Koin
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
 * Live wishlist smoke run (M9.2) on the local test instance. Run it with
 * `./gradlew :composeApp:smokeWishlistRun`.
 *
 * It creates a top-level wishlist and a child, renames the child, and uploads, replaces and
 * removes the child's image. It prints whether `GET /api/wishlists` lists the child too. It
 * creates a wish in the child with every field, uploads its image and replaces it, edits two
 * fields and sends an explicit `null` for `url`. It sends a wish with currency `EURO`, one with an
 * empty name, and one without `wishlist`. Last it deletes the parent: the child and the wish must
 * give 404, and the count of wishlists must end as it started.
 *
 * The repository always sends `wishlist`, so the create without it goes through the app's own
 * `HttpClient`. If the run fails halfway, it still deletes what it created.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    val created = WishlistRunCreated()
    val exitCode = try {
        runBlocking { run(koin, created) }
        0
    } catch (e: Exception) {
        System.err.println("smoke wishlist run failed: $e")
        1
    } finally {
        runBlocking { cleanUp(koin, created) }
        stopKoin()
    }
    exitProcess(exitCode)
}

/** What the run made and has not deleted yet. */
private class WishlistRunCreated {
    var parentId: String? = null
    var strayWishId: String? = null
}

@Suppress("LongMethod")
private suspend fun run(koin: Koin, created: WishlistRunCreated) {
    val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
    println("login: $login")
    check(login == LoginResult.LoggedIn) { "login failed" }

    val wishlists = koin.get<WishlistsRepository>()
    val client = koin.get<HttpClient>()
    val stamp = System.currentTimeMillis()
    val countBefore = wishlists.getWishlists().getOrThrow().size
    println("wishlists before: $countBefore")

    val parent = wishlists.createWishlist("M9.2 smoke parent $stamp").getOrThrow()
    created.parentId = parent.id
    println("created parent: $parent")
    check(parent.parentId == null) { "the parent is not top level" }

    val child = wishlists.createWishlist("M9.2 smoke child $stamp", parent.id).getOrThrow()
    println("created child: $child")
    check(child.parentId == parent.id) { "the child has parent ${child.parentId}" }
    check(wishlists.getChildren(parent.id).getOrThrow().map { it.id } == listOf(child.id)) {
        "the parent does not list the child"
    }

    val listed = wishlists.getWishlists().getOrThrow().map { it.id }
    println("GET /api/wishlists lists the parent: ${parent.id in listed}, the child: ${child.id in listed}")

    val renamed = wishlists.renameWishlist(child.id, "M9.2 smoke child renamed $stamp").getOrThrow()
    println("rename: ${renamed.name}")
    check(wishlists.getWishlist(child.id).getOrThrow().name == renamed.name) { "rename not read back" }
    check(renamed.parentId == parent.id) { "the rename changed the parent" }

    HttpClient().use { files ->
        val first = wishlists.uploadWishlistImage(child.id, jpeg(Color.ORANGE), "w1.jpg", JPEG)
            .getOrThrow().imageUrl
            .let { checkNotNull(it) { "no wishlist image after the upload" } }
        println("wishlist image uploaded: $first")
        expectStatus(files, first, HttpStatusCode.OK)

        val second = wishlists.uploadWishlistImage(child.id, jpeg(Color.GREEN), "w2.jpg", JPEG)
            .getOrThrow().imageUrl
            .let { checkNotNull(it) { "no wishlist image after the replace" } }
        println("wishlist image replaced: $second")
        check(second != first) { "wishlist image URL did not change" }
        expectStatus(files, second, HttpStatusCode.OK)
        expectStatus(files, first, HttpStatusCode.NotFound)

        val removed = wishlists.deleteWishlistImage(child.id).getOrThrow()
        println("wishlist image removed: imageUrl = ${removed.imageUrl}")
        check(removed.imageUrl == null) { "image still set in the PATCH answer" }
        check(wishlists.getWishlist(child.id).getOrThrow().imageUrl == null) { "image still set on read" }
        expectStatus(files, second, HttpStatusCode.NotFound)
    }

    val wish = wishlists.createWish(
        wishlistId = child.id,
        name = "M9.2 smoke wish $stamp",
        url = "https://example.com/book",
        price = "12.50",
        currency = "EUR",
        comment = "first comment"
    ).getOrThrow()
    println("created wish: $wish")
    check(wish.wishlistId == child.id) { "the wish is in ${wish.wishlistId}" }
    check(
        listOf(wish.url, wish.price, wish.currency, wish.comment) ==
            listOf("https://example.com/book", "12.50", "EUR", "first comment")
    ) { "the wish fields were not stored as sent" }
    check(wishlists.getWishes(child.id).getOrThrow().map { it.id } == listOf(wish.id)) {
        "the child does not list the wish"
    }

    HttpClient().use { files ->
        val first = wishlists.uploadWishImage(wish.id, jpeg(Color.BLUE), "h1.jpg", JPEG)
            .getOrThrow().imageUrl
            .let { checkNotNull(it) { "no wish image after the upload" } }
        println("wish image uploaded: $first")
        expectStatus(files, first, HttpStatusCode.OK)

        val second = wishlists.uploadWishImage(wish.id, jpeg(Color.RED), "h2.jpg", JPEG)
            .getOrThrow().imageUrl
            .let { checkNotNull(it) { "no wish image after the replace" } }
        println("wish image replaced: $second")
        check(second != first) { "wish image URL did not change" }
        expectStatus(files, second, HttpStatusCode.OK)
        expectStatus(files, first, HttpStatusCode.NotFound)
    }

    val edited = wishlists.editWish(wish.id, mapOf(WishField.PRICE to "9.99", WishField.COMMENT to "second comment"))
        .getOrThrow()
    println("edit price and comment: price = ${edited.price}, comment = ${edited.comment}")
    val read = wishlists.getWish(wish.id).getOrThrow()
    check(read.price == "9.99" && read.comment == "second comment") { "the edit was not read back" }
    check(read.name == wish.name && read.url == wish.url && read.currency == wish.currency) {
        "the edit changed a field it did not send"
    }

    val nullUrl = wishlists.editWish(wish.id, mapOf(WishField.URL to null))
    println("edit url = null: ${nullUrl.exceptionOrNull() ?: "url = ${nullUrl.getOrNull()?.url}"}")
    println("  url on read after it: ${wishlists.getWish(wish.id).getOrThrow().url}")

    val euro = wishlists.createWish(child.id, "M9.2 smoke euro $stamp", currency = "EURO").exceptionOrNull()
    println("create with currency EURO: $euro")
    check(euro is KoillectionError.Validation) { "currency EURO was not rejected with 422" }

    val empty = wishlists.createWish(child.id, "").exceptionOrNull()
    println("create with an empty name: $empty")
    check(empty is KoillectionError.Validation) { "an empty name was not rejected with 422" }

    val noWishlist = runCatching {
        client.post("api/wishes") {
            contentType(ContentTypeLdJson)
            setBody(buildJsonObject { put("name", "M9.2 smoke no wishlist $stamp") })
        }.body<JsonObject>()
    }
    noWishlist.getOrNull()?.let { created.strayWishId = it["id"]?.jsonPrimitive?.content }
    println("create without wishlist: ${noWishlist.exceptionOrNull() ?: noWishlist.getOrNull()}")

    wishlists.deleteWishlist(parent.id).getOrThrow()
    println("DELETE parent ${parent.id}: ok")
    created.parentId = null
    expectNotFound("parent", wishlists.getWishlist(parent.id).exceptionOrNull())
    expectNotFound("child", wishlists.getWishlist(child.id).exceptionOrNull())
    expectNotFound("wish", wishlists.getWish(wish.id).exceptionOrNull())

    val countAfter = wishlists.getWishlists().getOrThrow().size
    println("wishlists after: $countAfter")
    check(countAfter == countBefore) { "the test instance does not end as it started" }
}

/** Deletes what a failed run left. A wishlist delete takes its children and wishes with it. */
private suspend fun cleanUp(koin: Koin, created: WishlistRunCreated) {
    val wishlists = koin.get<WishlistsRepository>()
    created.strayWishId?.let { id ->
        wishlists.deleteWish(id).onFailure { System.err.println("cleanup delete of wish $id failed: $it") }
    }
    created.parentId?.let { id ->
        wishlists.deleteWishlist(id).onFailure { System.err.println("cleanup delete of wishlist $id failed: $it") }
    }
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
