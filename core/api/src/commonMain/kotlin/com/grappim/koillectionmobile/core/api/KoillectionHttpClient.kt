package com.grappim.koillectionmobile.core.api

import com.grappim.kit.customheaders.CustomHeaders
import com.grappim.kit.customheaders.CustomHeadersStorage
import com.grappim.kit.customheaders.sanitizeCustomHeaders
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.SessionStorage
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.ContentTypeMatcher
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

/** API Platform's default format. Lists come back as Hydra collections in it (plan §2). */
val ContentTypeLdJson = ContentType("application", "ld+json")

/** The only body type `PATCH` takes (plan §2). */
val ContentTypeMergePatchJson = ContentType("application", "merge-patch+json")

/**
 * The configuration of the app's one `HttpClient`. The Koin provider applies it to a client on
 * the platform engine; tests apply it to a `MockEngine` client.
 *
 * - Base URL per request from [baseUrlProvider]; paths are relative (`api/collections`).
 * - JSON: `Accept: application/ld+json` by default, because plain `application/json` lists
 *   lose the Hydra paging keys. Ktor serializes a request body only when the request sets its
 *   content type, so callers set one: [ContentTypeLdJson], or [ContentTypeMergePatchJson] for
 *   `PATCH`.
 * - `Authorization: Bearer` from [sessionStorage], and one new login on a 401 ([bearerAuth]).
 * - Every non-2xx status and every network failure becomes a [KoillectionError]. A proxy's
 *   answer ([isProxyRejection]) becomes [KoillectionError.ProxyRejection].
 */
fun HttpClientConfig<*>.configureKoillectionClient(
    json: Json,
    baseUrlProvider: BaseUrlProvider,
    sessionStorage: SessionStorage,
    customHeadersStorage: CustomHeadersStorage,
    logLevel: LogLevel
) {
    // The API does not redirect. A redirect is a proxy or a wrong URL, and `isProxyRejection`
    // must see it: Ktor would follow a GET to a proxy's login page.
    followRedirects = false
    defaultRequest {
        url(normalizeBaseUrl(baseUrlProvider.getBaseUrl()))
    }
    install(ContentNegotiation) {
        register(ContentTypeLdJson, KotlinxSerializationConverter(json), JsonFamilyMatcher) {}
    }
    install(CustomHeaders) {
        storage = customHeadersStorage
        serverUrl = { normalizeBaseUrl(baseUrlProvider.getBaseUrl()) }
    }
    install(bearerAuth(sessionStorage))
    install(Logging) {
        logger = RedactingLogger(tag = "Ktor")
        level = logLevel
        sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
        sanitizeCustomHeaders(customHeadersStorage)
    }
    HttpResponseValidator {
        validateResponse { response ->
            val status = response.status.value
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                val location = response.headers[HttpHeaders.Location]
                if (isProxyRejection(status, body, location, response.request.url.host)) {
                    throw KoillectionError.ProxyRejection(status)
                }
                throw mapHttpError(status, body, json)
            }
        }
        handleResponseExceptionWithRequest { cause, _ ->
            if (cause is IOException) throw KoillectionError.Network(cause)
        }
    }
}

/** `application/json` and every `application/<x>+json` type: ld+json, merge-patch+json, problem+json. */
private object JsonFamilyMatcher : ContentTypeMatcher {
    override fun contains(contentType: ContentType): Boolean {
        val type = contentType.withoutParameters()
        return type.contentType == "application" &&
            (type.contentSubtype == "json" || type.contentSubtype.endsWith("+json"))
    }
}
