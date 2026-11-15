package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin
import kotlin.system.exitProcess

/**
 * The local test instance from `docs/local-info.md`. Never the user's real server: it holds the
 * working book catalogue. The run only reads, but it logs in and stores a session.
 */
private const val SERVER_URL = "http://127.0.0.1:8290"
private const val USERNAME = "tester"
private const val PASSWORD = "koillection-test"

/**
 * Live read-only smoke run (M1.11): logs in to the local test instance, then reads collections,
 * the items of the first collection that has items, and the fields of its first item, through the
 * real repositories and the real JVM Koin graph. Run it with `./gradlew :composeApp:smokeRun`.
 *
 * It is a `main`, not a test, so `build` and CI compile it but never run it. The Gradle task
 * points `user.home` at a build directory, so the session does not go to `~/.koillectionmobile`.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    val exitCode = try {
        runBlocking {
            val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
            println("login: $login")
            check(login == LoginResult.LoggedIn) { "login failed" }

            val collections = koin.get<CollectionsRepository>().getCollections().getOrThrow()
            println("collections: ${collections.size}")
            collections.forEach { println("  ${it.title} (parent: ${it.parentId ?: "-"})") }

            val itemsRepository = koin.get<ItemsRepository>()
            // The first collection may hold only child collections, so look for one with items.
            var found: Pair<Collection, List<Item>>? = null
            for (candidate in collections) {
                val candidateItems = itemsRepository.getItems(candidate.id).getOrThrow()
                if (candidateItems.isNotEmpty()) {
                    found = candidate to candidateItems
                    break
                }
            }
            val (collection, items) = checkNotNull(found) { "no collection has items" }
            println("items in '${collection.title}': ${items.size}")
            items.take(ITEMS_SHOWN).forEach { println("  ${it.name}") }

            val item = itemsRepository.getItem(items.first().id).getOrThrow()
            println("item: ${item.name}, image: ${item.image?.url ?: "-"}")
            itemsRepository.getData(item.id).getOrThrow().forEach {
                println("  [${it.position}] ${it.type} ${it.label}: ${it.value ?: it.image?.url ?: "-"}")
            }
        }
        0
    } catch (e: Exception) {
        System.err.println("smoke run failed: $e")
        1
    } finally {
        stopKoin()
    }
    exitProcess(exitCode)
}

private const val ITEMS_SHOWN = 5
