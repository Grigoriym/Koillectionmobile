package com.grappim.koillectionmobile.feature.auth.data

import com.grappim.koillectionmobile.core.api.LOGIN_PATH
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.storage.Credentials
import com.grappim.koillectionmobile.core.storage.SessionStorage
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.auth.dto.LoginRequestDto
import com.grappim.koillectionmobile.feature.auth.dto.LoginResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parseUrl
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * The login is one request, `POST /api/authentication_token`, and its answer is also the server-URL
 * check: only Koillection answers there with a `token` or a JWT-layer 401. There is no separate
 * probe request, because any request other than the login carries the stored JWT, and the typed
 * URL can point at a different server.
 *
 * The URL is saved before the request: requests resolve against the stored URL, and the custom
 * headers go only to the stored URL's origin. A failed login puts the previous URL back.
 */
@Single(binds = [AuthRepository::class])
internal class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    private val json: Json
) : AuthRepository {

    override suspend fun logIn(serverUrl: String, username: String, password: String): LoginResult {
        val url = serverUrl.trim()
        if (!isServerUrl(url)) return LoginResult.InvalidServerUrl

        val previousUrl = sessionStorage.serverUrl
        sessionStorage.saveServerUrl(url)
        val result = try {
            requestToken(username, password)?.let { token ->
                sessionStorage.saveCredentials(Credentials(username = username, password = password))
                sessionStorage.saveToken(token)
                LoginResult.LoggedIn
            } ?: LoginResult.NotKoillection
        } catch (error: KoillectionError) {
            error.toLoginResult()
        }
        if (result != LoginResult.LoggedIn) sessionStorage.saveServerUrl(previousUrl)
        return result
    }

    override suspend fun logOut() {
        sessionStorage.clearSession()
    }

    override fun storedServerUrl(): String = sessionStorage.serverUrl

    /** `null` when a 2xx body has no `token`: something other than Koillection answered. */
    private suspend fun requestToken(username: String, password: String): String? {
        // `application/json`, as `core:api`'s re-login sends it. The server answers plain JSON.
        val body = httpClient.post(LOGIN_PATH) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(username = username, password = password))
        }.bodyAsText()
        return try {
            json.decodeFromString<LoginResponseDto>(body).token
        } catch (_: IllegalArgumentException) {
            // `SerializationException` (an HTML page, a JSON body without `token`) extends it.
            null
        }
    }
}

private fun KoillectionError.toLoginResult(): LoginResult = when {
    this is KoillectionError.Unauthorized -> LoginResult.InvalidCredentials
    this is KoillectionError.Server && status == INTERNAL_SERVER_ERROR -> LoginResult.ServerJwtFailure
    this is KoillectionError.Network -> LoginResult.Unreachable
    this is KoillectionError.NotFound -> LoginResult.NotKoillection
    this is KoillectionError.Http && status == METHOD_NOT_ALLOWED -> LoginResult.NotKoillection
    else -> LoginResult.Failed(this)
}

private val INTERNAL_SERVER_ERROR = HttpStatusCode.InternalServerError.value
private val METHOD_NOT_ALLOWED = HttpStatusCode.MethodNotAllowed.value

/**
 * The scheme and the host must be typed. Ktor's parser fills in what is missing: it reads
 * `koi.example` as a relative path on `localhost`, and gives `https:///sub` a host too. So the host
 * is read from the text itself, and the parser only rejects what is malformed.
 */
internal fun isServerUrl(url: String): Boolean {
    val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return false
    val host = url.substringAfter("://").takeWhile { it !in "/?#" }
    return host.isNotBlank() && parseUrl(url) != null
}
