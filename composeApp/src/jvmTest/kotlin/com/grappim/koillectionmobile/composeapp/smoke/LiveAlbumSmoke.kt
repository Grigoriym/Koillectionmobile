package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
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
 * Live album smoke run (M11.2) on the local test instance. Run it with
 * `./gradlew :composeApp:smokeAlbumRun`.
 *
 * It creates a top-level album and a child, renames the child, and uploads, replaces and removes
 * the child's image. It prints whether `GET /api/albums` lists the child too. It creates a photo in
 * the child with every field, uploads its image and replaces it, and prints the raw `takenAt` and
 * `imageSmallThumbnail`. It edits two fields and sends an explicit `null` for `comment`. It sends a
 * photo with an empty title and one without `album`. Last it deletes the parent: the child and the
 * photo must give 404, and the count of albums must end as it started.
 *
 * The repository always sends `album`, so the create without it goes through the app's own
 * `HttpClient`, as does the raw read of the photo. If the run fails halfway, it still deletes what
 * it created.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    val created = AlbumRunCreated()
    val exitCode = try {
        runBlocking { run(koin, created) }
        0
    } catch (e: Exception) {
        System.err.println("smoke album run failed: $e")
        1
    } finally {
        runBlocking { cleanUp(koin, created) }
        stopKoin()
    }
    exitProcess(exitCode)
}

/** What the run made and has not deleted yet. */
private class AlbumRunCreated {
    var parentId: String? = null
    var strayPhotoId: String? = null
}

@Suppress("LongMethod")
private suspend fun run(koin: Koin, created: AlbumRunCreated) {
    val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
    println("login: $login")
    check(login == LoginResult.LoggedIn) { "login failed" }

    val albums = koin.get<AlbumsRepository>()
    val client = koin.get<HttpClient>()
    val stamp = System.currentTimeMillis()
    val countBefore = albums.getAlbums().getOrThrow().size
    println("albums before: $countBefore")

    val parent = albums.createAlbum("M11.2 smoke parent $stamp").getOrThrow()
    created.parentId = parent.id
    println("created parent: $parent")
    check(parent.parentId == null) { "the parent is not top level" }

    val child = albums.createAlbum("M11.2 smoke child $stamp", parent.id).getOrThrow()
    println("created child: $child")
    check(child.parentId == parent.id) { "the child has parent ${child.parentId}" }
    check(albums.getChildren(parent.id).getOrThrow().map { it.id } == listOf(child.id)) {
        "the parent does not list the child"
    }

    val listed = albums.getAlbums().getOrThrow().map { it.id }
    println("GET /api/albums lists the parent: ${parent.id in listed}, the child: ${child.id in listed}")

    val renamed = albums.renameAlbum(child.id, "M11.2 smoke child renamed $stamp").getOrThrow()
    println("rename: ${renamed.title}")
    check(albums.getAlbum(child.id).getOrThrow().title == renamed.title) { "rename not read back" }
    check(renamed.parentId == parent.id) { "the rename changed the parent" }

    HttpClient().use { files ->
        val first = albums.uploadAlbumImage(child.id, jpeg(Color.ORANGE), "a1.jpg", JPEG)
            .getOrThrow().imageUrl
            .let { checkNotNull(it) { "no album image after the upload" } }
        println("album image uploaded: $first")
        expectStatus(files, first, HttpStatusCode.OK)

        val second = albums.uploadAlbumImage(child.id, jpeg(Color.GREEN), "a2.jpg", JPEG)
            .getOrThrow().imageUrl
            .let { checkNotNull(it) { "no album image after the replace" } }
        println("album image replaced: $second")
        check(second != first) { "album image URL did not change" }
        expectStatus(files, second, HttpStatusCode.OK)
        expectStatus(files, first, HttpStatusCode.NotFound)

        val removed = albums.deleteAlbumImage(child.id).getOrThrow()
        println("album image removed: imageUrl = ${removed.imageUrl}")
        check(removed.imageUrl == null) { "image still set in the PATCH answer" }
        check(albums.getAlbum(child.id).getOrThrow().imageUrl == null) { "image still set on read" }
        expectStatus(files, second, HttpStatusCode.NotFound)
    }

    val photo = albums.createPhoto(
        albumId = child.id,
        title = "M11.2 smoke photo $stamp",
        comment = "first comment",
        place = "first place"
    ).getOrThrow()
    println("created photo: $photo")
    check(photo.albumId == child.id) { "the photo is in ${photo.albumId}" }
    check(listOf(photo.comment, photo.place) == listOf("first comment", "first place")) {
        "the photo fields were not stored as sent"
    }
    check(albums.getPhotos(child.id).getOrThrow().map { it.id } == listOf(photo.id)) {
        "the child does not list the photo"
    }
    printRaw(client, photo.id, "before the upload")

    HttpClient().use { files ->
        val first = albums.uploadPhotoImage(photo.id, jpeg(Color.BLUE), "p1.jpg", JPEG).getOrThrow()
        val firstImage = checkNotNull(first.imageUrl) { "no photo image after the upload" }
        println("photo image uploaded: image = $firstImage, thumbnail = ${first.thumbnailUrl}")
        expectStatus(files, firstImage, HttpStatusCode.OK)
        first.thumbnailUrl?.let { expectStatus(files, it, HttpStatusCode.OK) }
        printRaw(client, photo.id, "after the upload")

        val second = albums.uploadPhotoImage(photo.id, jpeg(Color.RED), "p2.jpg", JPEG).getOrThrow()
        val secondImage = checkNotNull(second.imageUrl) { "no photo image after the replace" }
        println("photo image replaced: image = $secondImage, thumbnail = ${second.thumbnailUrl}")
        check(secondImage != firstImage) { "photo image URL did not change" }
        expectStatus(files, secondImage, HttpStatusCode.OK)
        expectStatus(files, firstImage, HttpStatusCode.NotFound)
        printRaw(client, photo.id, "after the replace")
    }

    val edited = albums.editPhoto(
        photo.id,
        mapOf(PhotoField.TITLE to "M11.2 smoke photo edited $stamp", PhotoField.PLACE to "second place")
    ).getOrThrow()
    println("edit title and place: title = ${edited.title}, place = ${edited.place}")
    val read = albums.getPhoto(photo.id).getOrThrow()
    check(read.title == edited.title && read.place == "second place") { "the edit was not read back" }
    check(read.comment == photo.comment && read.albumId == child.id && read.imageUrl != null) {
        "the edit changed a field it did not send"
    }

    val nullComment = albums.editPhoto(photo.id, mapOf(PhotoField.COMMENT to null))
    println("edit comment = null: ${nullComment.exceptionOrNull() ?: "comment = ${nullComment.getOrNull()?.comment}"}")
    println("  comment on read after it: ${albums.getPhoto(photo.id).getOrThrow().comment}")

    val empty = albums.createPhoto(child.id, "").exceptionOrNull()
    println("create with an empty title: $empty")
    check(empty is KoillectionError.Validation) { "an empty title was not rejected with 422" }

    val noAlbum = runCatching {
        client.post("api/photos") {
            contentType(ContentTypeLdJson)
            setBody(buildJsonObject { put("title", "M11.2 smoke no album $stamp") })
        }.body<JsonObject>()
    }
    noAlbum.getOrNull()?.let { created.strayPhotoId = it["id"]?.jsonPrimitive?.content }
    println("create without album: ${noAlbum.exceptionOrNull() ?: noAlbum.getOrNull()}")

    albums.deleteAlbum(parent.id).getOrThrow()
    println("DELETE parent ${parent.id}: ok")
    created.parentId = null
    expectNotFound("parent", albums.getAlbum(parent.id).exceptionOrNull())
    expectNotFound("child", albums.getAlbum(child.id).exceptionOrNull())
    expectNotFound("photo", albums.getPhoto(photo.id).exceptionOrNull())

    val countAfter = albums.getAlbums().getOrThrow().size
    println("albums after: $countAfter")
    check(countAfter == countBefore) { "the test instance does not end as it started" }
}

/** Prints the photo's `takenAt`, `image` and `imageSmallThumbnail` as the server sends them. */
private suspend fun printRaw(client: HttpClient, id: String, moment: String) {
    val raw = client.get("api/photos/$id").body<JsonObject>()
    println(
        "  raw photo $moment: takenAt = ${raw["takenAt"]}, image = ${raw["image"]}, " +
            "imageSmallThumbnail = ${raw["imageSmallThumbnail"]}"
    )
}

/** Deletes what a failed run left. An album delete takes its children and photos with it. */
private suspend fun cleanUp(koin: Koin, created: AlbumRunCreated) {
    val albums = koin.get<AlbumsRepository>()
    created.strayPhotoId?.let { id ->
        albums.deletePhoto(id).onFailure { System.err.println("cleanup delete of photo $id failed: $it") }
    }
    created.parentId?.let { id ->
        albums.deleteAlbum(id).onFailure { System.err.println("cleanup delete of album $id failed: $it") }
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
