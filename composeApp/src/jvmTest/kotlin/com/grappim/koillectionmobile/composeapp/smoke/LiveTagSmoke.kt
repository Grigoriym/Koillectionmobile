package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
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

/**
 * Live tag smoke run (M7.4) on the local test instance. It finds an item that already has tags,
 * creates a tag, sets it on the item next to the existing tags, reads `/api/tags/{id}/items`,
 * takes the tag off the item again and deletes the tag. It also sends a create with an empty label
 * (must be rejected) and a create with a label that exists already (prints what the server does).
 * Run it with `./gradlew :composeApp:smokeTagRun`.
 *
 * The app has no tag delete call, so the run sends `DELETE /api/tags/{id}` through the app's own
 * `HttpClient`. If the run fails halfway, it still puts the item's tags back and deletes the tags
 * it created.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    val createdTagIds = mutableListOf<String>()
    var restore: Pair<String, List<String>>? = null
    val exitCode = try {
        runBlocking {
            val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
            println("login: $login")
            check(login == LoginResult.LoggedIn) { "login failed" }

            val tags = koin.get<TagsRepository>()
            val items = koin.get<ItemsRepository>()
            val stamp = System.currentTimeMillis()

            val item = tags.getTags().getOrThrow().firstNotNullOfOrNull { tag ->
                items.getTagItems(tag.id).getOrThrow().firstOrNull()
            }.let { checkNotNull(it) { "no item on the test instance has a tag" } }
            val before = tags.getItemTags(item.id).getOrThrow()
            println("item: ${item.name} (${item.id}), tags before: ${before.map { it.label }}")

            val tag = tags.createTag("M7.4 smoke $stamp").getOrThrow()
            createdTagIds += tag.id
            println("created tag: $tag")

            restore = item.id to before.map { it.id }
            tags.setItemTags(item.id, before.map { it.id } + tag.id).getOrThrow()
            val withTag = tags.getItemTags(item.id).getOrThrow()
            println("tags with the new one: ${withTag.map { it.label }}")
            check(withTag.map { it.id }.toSet() == before.map { it.id }.toSet() + tag.id) {
                "the item does not have the old tags plus the new one"
            }

            val tagItems = items.getTagItems(tag.id).getOrThrow()
            println("GET /api/tags/${tag.id}/items: ${tagItems.map { it.name }}")
            check(tagItems.map { it.id } == listOf(item.id)) { "the tag's items are not exactly the item" }

            tags.setItemTags(item.id, before.map { it.id }).getOrThrow()
            restore = null
            val after = tags.getItemTags(item.id).getOrThrow()
            println("tags after the removal: ${after.map { it.label }}")
            check(after.map { it.id }.toSet() == before.map { it.id }.toSet()) { "the item's tags changed" }
            check(items.getTagItems(tag.id).getOrThrow().isEmpty()) { "the tag still has items" }

            val empty = tags.createTag("").exceptionOrNull()
            println("create with an empty label: $empty")
            check(empty is KoillectionError.Validation) { "an empty label was not rejected with 422" }

            val duplicate = tags.createTag(tag.label)
            println("create with an existing label: $duplicate")
            duplicate.getOrNull()?.let {
                createdTagIds += it.id
                println("  second tag: same label ${it.label == tag.label}, new id ${it.id != tag.id}")
            }

            createdTagIds.toList().forEach { id ->
                deleteTag(koin, id)
                createdTagIds -= id
            }
        }
        0
    } catch (e: Exception) {
        System.err.println("smoke tag run failed: $e")
        1
    } finally {
        runBlocking {
            restore?.let { (itemId, tagIds) ->
                koin.get<TagsRepository>().setItemTags(itemId, tagIds)
                    .onFailure { System.err.println("cleanup: restoring the tags of item $itemId failed: $it") }
            }
            createdTagIds.forEach { id ->
                runCatching { deleteTag(koin, id) }
                    .onFailure { System.err.println("cleanup delete of tag $id failed: $it") }
            }
        }
        stopKoin()
    }
    exitProcess(exitCode)
}

/** Deletes tag [id] and checks that a read of it gives 404 after. */
private suspend fun deleteTag(koin: Koin, id: String) {
    val client = koin.get<HttpClient>()
    println("DELETE /api/tags/$id: ${client.delete("api/tags/$id").status}")
    val read = runCatching { client.get("api/tags/$id") }.exceptionOrNull()
    println("  GET the tag after the delete: $read")
    check(read is KoillectionError.NotFound) { "tag $id still readable after the delete" }
}
