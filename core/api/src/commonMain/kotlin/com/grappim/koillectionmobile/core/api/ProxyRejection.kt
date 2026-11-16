package com.grappim.koillectionmobile.core.api

import io.ktor.http.HttpStatusCode
import io.ktor.http.parseUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * A response that came from a proxy in front of the server, not from Koillection (plan §2).
 *
 * Checked against 1.8.5 and an nginx front (2026-11-16):
 * - Koillection answers every 401 through the JWT layer: `application/json`,
 *   `WWW-Authenticate: Bearer`, and the body `{"code":401,"message":...}`. Only that body
 *   is Koillection's own 401. nginx sends an HTML page for `return 401` and for `auth_basic`.
 * - A 407 is always a proxy.
 * - The API does not redirect. A redirect to another host is a proxy that sends the app to its
 *   login page. A redirect on the same host (`http` → `https`) is a wrong URL, not a proxy.
 *
 * @param requestHost the host that the request went to.
 */
internal fun isProxyRejection(status: Int, body: String, location: String?, requestHost: String): Boolean =
    when (status) {
        HttpStatusCode.Unauthorized.value -> !isKoillectionUnauthorized(body)
        HttpStatusCode.ProxyAuthenticationRequired.value -> true
        in HTTP_REDIRECTS -> isRedirectOffHost(location, requestHost)
        else -> false
    }

/** The JWT layer's 401 body: a JSON object with `"code": 401`. */
internal fun isKoillectionUnauthorized(body: String): Boolean {
    val code = try {
        Json.parseToJsonElement(body).jsonObject["code"] as? JsonPrimitive
    } catch (_: IllegalArgumentException) {
        // `SerializationException` (not JSON) and the non-object case both extend it.
        return false
    }
    return code?.intOrNull == HttpStatusCode.Unauthorized.value
}

private fun isRedirectOffHost(location: String?, requestHost: String): Boolean {
    if (location == null) return false
    val isAbsolute = location.contains(SCHEME_SEPARATOR) || location.startsWith(PROTOCOL_RELATIVE)
    if (!isAbsolute) return false
    val target = parseUrl(if (location.startsWith(PROTOCOL_RELATIVE)) "http:$location" else location)
    return target != null && !target.host.equals(requestHost, ignoreCase = true)
}

private val HTTP_REDIRECTS = 300..399
private const val SCHEME_SEPARATOR = "://"
private const val PROTOCOL_RELATIVE = "//"
