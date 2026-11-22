package com.grappim.koillectionmobile.composeapp.smoke

import com.grappim.koillectionmobile.composeapp.di.KoinApp
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.loans.domain.LoanChange
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
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
private const val COLLECTION_TITLE = "Books"

private val LENT_AT = LocalDate(2026, 11, 20)
private val RETURNED_AT = LocalDate(2026, 11, 21)

/**
 * Live loan smoke run (M10.2) on the local test instance. Run it with
 * `./gradlew :composeApp:smokeLoanRun`.
 *
 * In collection `Books` it creates a throwaway item and two loans on it, the first with a
 * date-only `lentAt`. It prints the raw `lentAt` the server gives back. It edits `lentTo`, sets
 * `returnedAt` and clears it with an explicit `null`. It sends an empty `lentTo` and a loan
 * without `lentAt` (422 each). It lists the item's loans, deletes the first loan, then deletes
 * the item: the second loan must give 404. The counts of loans and of items in `Books` must end
 * as they started.
 *
 * The repository always sends `lentAt`, so the create without it goes through the app's own
 * `HttpClient`. If the run fails halfway, it still deletes what it created.
 */
fun main() {
    val koin = startKoin<KoinApp> { }.koin
    val created = LoanRunCreated()
    val exitCode = try {
        runBlocking { run(koin, created) }
        0
    } catch (e: Exception) {
        System.err.println("smoke loan run failed: $e")
        1
    } finally {
        runBlocking { cleanUp(koin, created) }
        stopKoin()
    }
    exitProcess(exitCode)
}

/** What the run made and has not deleted yet. */
private class LoanRunCreated {
    var itemId: String? = null
    var strayLoanId: String? = null
}

@Suppress("LongMethod")
private suspend fun run(koin: Koin, created: LoanRunCreated) {
    val login = koin.get<AuthRepository>().logIn(SERVER_URL, USERNAME, PASSWORD)
    println("login: $login")
    check(login == LoginResult.LoggedIn) { "login failed" }

    val client = koin.get<HttpClient>()
    val items = koin.get<ItemsRepository>()
    val loans = koin.get<LoansRepository>()
    val collection = koin.get<CollectionsRepository>().getCollections().getOrThrow()
        .single { it.title == COLLECTION_TITLE }
    val stamp = System.currentTimeMillis()

    val loansBefore = client.getAllHydraPages<JsonObject>("api/loans").size
    val itemsBefore = items.getItems(collection.id).getOrThrow().size
    println("loans before: $loansBefore, items in $COLLECTION_TITLE before: $itemsBefore")

    val item = items.createItem(collection.id, "M10.2 smoke $stamp").getOrThrow()
    created.itemId = item.id
    println("created item: ${item.id}")

    val first = loans.createLoan(item.id, "M10.2 smoke first", LENT_AT).getOrThrow()
    println("created loan: $first")
    println("  raw lentAt on read: ${rawLoan(client, first.id)["lentAt"]}")
    check(first.itemId == item.id) { "the loan is on item ${first.itemId}" }
    check(first.lentAt == LENT_AT) { "lentAt $LENT_AT came back as ${first.lentAt}" }
    check(first.returnedAt == null) { "a new loan is returned: ${first.returnedAt}" }

    val second = loans.createLoan(item.id, "M10.2 smoke second", LENT_AT).getOrThrow()
    println("created loan: $second")

    val renamed = loans.editLoan(first.id, listOf(LoanChange.LentTo("M10.2 smoke renamed"))).getOrThrow()
    println("edit lentTo: $renamed")
    check(renamed.lentTo == "M10.2 smoke renamed") { "lentTo not changed" }
    check(renamed.lentAt == LENT_AT && renamed.returnedAt == null) { "the edit changed a field it did not send" }

    val returned = loans.editLoan(first.id, listOf(LoanChange.ReturnedAt(RETURNED_AT))).getOrThrow()
    println("set returnedAt: $returned")
    println("  raw returnedAt on read: ${rawLoan(client, first.id)["returnedAt"]}")
    check(returned.returnedAt == RETURNED_AT) { "returnedAt $RETURNED_AT came back as ${returned.returnedAt}" }
    check(returned.lentTo == renamed.lentTo && returned.lentAt == LENT_AT) {
        "the edit changed a field it did not send"
    }

    val cleared = loans.editLoan(first.id, listOf(LoanChange.ReturnedAt(null))).getOrThrow()
    println("clear returnedAt: $cleared")
    println("  raw returnedAt on read: ${rawLoan(client, first.id)["returnedAt"]}")
    check(cleared.returnedAt == null) { "returnedAt not cleared in the PATCH answer" }

    val empty = loans.createLoan(item.id, "", LENT_AT).exceptionOrNull()
    println("create with an empty lentTo: $empty")
    check(empty is KoillectionError.Validation) { "an empty lentTo was not rejected with 422" }

    val noLentAt = runCatching {
        client.post("api/loans") {
            contentType(ContentTypeLdJson)
            setBody(
                buildJsonObject {
                    put("item", "/api/items/${item.id}")
                    put("lentTo", "M10.2 smoke no lentAt")
                }
            )
        }.body<JsonObject>()
    }
    noLentAt.getOrNull()?.let { created.strayLoanId = it["id"]?.jsonPrimitive?.content }
    println("create without lentAt: ${noLentAt.exceptionOrNull() ?: noLentAt.getOrNull()}")
    check(noLentAt.exceptionOrNull() is KoillectionError.Validation) {
        "a loan without lentAt was not rejected with 422"
    }

    val listed = loans.getLoans(item.id).getOrThrow()
    println("loans of the item: ${listed.map { "${it.id} ${it.lentTo}" }}")
    check(listed.map { it.id }.toSet() == setOf(first.id, second.id)) { "the item does not list both loans" }

    loans.deleteLoan(first.id).getOrThrow()
    println("DELETE loan ${first.id}: ok")
    expectNotFound("first loan", client, first.id)

    items.deleteItem(item.id).getOrThrow()
    println("DELETE item ${item.id}: ok")
    created.itemId = null
    expectNotFound("second loan", client, second.id)

    val loansAfter = client.getAllHydraPages<JsonObject>("api/loans").size
    val itemsAfter = items.getItems(collection.id).getOrThrow().size
    println("loans after: $loansAfter, items in $COLLECTION_TITLE after: $itemsAfter")
    check(loansAfter == loansBefore && itemsAfter == itemsBefore) {
        "the test instance does not end as it started"
    }
}

/** Deletes what a failed run left. An item delete takes its loans with it. */
private suspend fun cleanUp(koin: Koin, created: LoanRunCreated) {
    created.strayLoanId?.let { id ->
        koin.get<LoansRepository>().deleteLoan(id)
            .onFailure { System.err.println("cleanup delete of loan $id failed: $it") }
    }
    created.itemId?.let { id ->
        koin.get<ItemsRepository>().deleteItem(id)
            .onFailure { System.err.println("cleanup delete of item $id failed: $it") }
    }
}

/** The loan as the server stores it, without the mapper: it shows the time and the offset. */
private suspend fun rawLoan(client: HttpClient, id: String): JsonObject = client.get("api/loans/$id").body()

private suspend fun expectNotFound(what: String, client: HttpClient, id: String) {
    val error = runCatching { rawLoan(client, id) }.exceptionOrNull()
    println("  GET $what after the delete: $error")
    check(error is KoillectionError.NotFound) { "$what still readable after the delete" }
}
