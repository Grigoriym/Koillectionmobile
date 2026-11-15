package com.grappim.koillectionmobile.core.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * One page of a Hydra list (plan §2). Koillection 1.8.5 (API Platform 5) sends the keys
 * without the `hydra:` prefix. Older servers send `hydra:member` and the other prefixed keys,
 * so each key also reads its prefixed name.
 *
 * - [view] is absent when the whole list fits on one page.
 * - [totalItems] counts the whole list, not this page.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HydraCollectionDto<T>(
    @JsonNames("hydra:member")
    val member: List<T> = emptyList(),
    @JsonNames("hydra:totalItems")
    val totalItems: Int? = null,
    @JsonNames("hydra:view")
    val view: HydraViewDto? = null
) {
    /** The next page's path and query, for example `/api/tags?page=2`. Null on the last page. */
    val nextPage: String? get() = view?.next
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HydraViewDto(
    @JsonNames("hydra:next")
    val next: String? = null
)

/**
 * Reads one Hydra list page at [path], then follows each `next` link until the last page.
 * Returns the members of all pages in server order.
 *
 * [path] is relative to the base URL (`api/tags`). A `next` link is an absolute path that
 * already holds the server's own prefix (`/koillection/api/tags?page=2`), so the client
 * requests it as it is: Ktor keeps the base URL's host and replaces its path.
 */
suspend inline fun <reified T> HttpClient.getAllHydraPages(path: String): List<T> {
    val members = mutableListOf<T>()
    var next: String? = path
    while (next != null) {
        val page = get(next).body<HydraCollectionDto<T>>()
        members += page.member
        next = page.nextPage
    }
    return members
}
