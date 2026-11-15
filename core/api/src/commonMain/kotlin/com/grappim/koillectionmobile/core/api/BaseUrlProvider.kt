package com.grappim.koillectionmobile.core.api

/**
 * The server URL the user entered: the instance root, for example `https://host/` or
 * `https://host/koillection/`. Request paths are relative to it (`api/collections`).
 *
 * Read per request, so a change of server applies at once. `NetworkModule` binds it to
 * `SessionStorage.serverUrl`.
 */
interface BaseUrlProvider {
    fun getBaseUrl(): String
}

/**
 * The trailing slash is needed. Ktor's `DefaultRequest` appends a relative path to the base
 * URL's path only when that path ends in `/`. Without it, `https://host/koillection` +
 * `api/collections` resolves to `https://host/api/collections` (wallosmobile's finding).
 */
internal fun normalizeBaseUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.isEmpty()) "" else trimmed.trimEnd('/') + "/"
}
