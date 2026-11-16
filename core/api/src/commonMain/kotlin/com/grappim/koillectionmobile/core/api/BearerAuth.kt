package com.grappim.koillectionmobile.core.api

import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * The JWT login endpoint (plan §2). Relative to the base URL. Public so that `feature:auth` logs in
 * on the same path that [bearerAuth] leaves without a bearer header.
 */
const val LOGIN_PATH = "api/authentication_token"

private val LOGIN_PATH_SEGMENTS = LOGIN_PATH.split('/')

/**
 * Sends `Authorization: Bearer <JWT>` with every request except the login request.
 *
 * Koillection has no refresh token. On a 401 from the JWT layer (not from a proxy,
 * [isKoillectionUnauthorized]) the plugin logs in again once with the stored
 * credentials, saves the new JWT and repeats the request once. A second 401 goes to the caller
 * as `KoillectionError.Unauthorized`, so there is no loop. A failed login goes to the caller as
 * its own error: a 500 there is the broken-JWT-keys trap (plan §2), not a session problem.
 *
 * A 401 at that login means the stored password no longer works. The plugin then clears the
 * session, so the app shows the login screen and not the same error on every screen.
 *
 * The mutex lets only one request log in at a time. A request that waited for it uses the JWT
 * that the other request saved.
 */
internal fun bearerAuth(sessionStorage: SessionStorage) = createClientPlugin("KoillectionBearerAuth") {
    val mutex = Mutex()

    on(Send) { request ->
        // Compare segments: at a server's root path the merged path has no leading `/` here.
        if (request.url.encodedPathSegments.takeLast(LOGIN_PATH_SEGMENTS.size) == LOGIN_PATH_SEGMENTS) {
            return@on proceed(request)
        }

        val sentToken = sessionStorage.getToken()
        sentToken?.let { request.headers[HttpHeaders.Authorization] = bearer(it) }
        val call = proceed(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@on call
        // A proxy's 401 is not about the JWT: a new login would only meet the same proxy.
        if (!isKoillectionUnauthorized(call.response.bodyAsText())) return@on call

        val newToken = mutex.withLock {
            sessionStorage.getToken().takeIf { it != sentToken } ?: logIn(client, sessionStorage)
        } ?: return@on call

        request.headers[HttpHeaders.Authorization] = bearer(newToken)
        proceed(request)
    }
}

/**
 * `null` when no credentials are stored. A failed login throws its `KoillectionError`, and a 401
 * clears the session first.
 */
private suspend fun logIn(client: HttpClient, sessionStorage: SessionStorage): String? {
    val credentials = sessionStorage.getCredentials() ?: return null
    val token = try {
        client.post(LOGIN_PATH) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = credentials.username, password = credentials.password))
        }.body<LoginResponse>().token
    } catch (error: KoillectionError.Unauthorized) {
        sessionStorage.clearSession()
        throw error
    }
    sessionStorage.saveToken(token)
    return token
}

private fun bearer(token: String) = "Bearer $token"

@Serializable
private class LoginRequest(val username: String, val password: String)

@Serializable
private class LoginResponse(val token: String)
