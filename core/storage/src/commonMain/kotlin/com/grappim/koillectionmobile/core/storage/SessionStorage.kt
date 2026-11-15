package com.grappim.koillectionmobile.core.storage

import kotlinx.coroutines.flow.Flow

/**
 * The login session: the server URL, the credentials and the JWT.
 *
 * The password is stored so that `core:api` can log in again when the JWT expires (a 401). The
 * server has no refresh token (plan §2).
 */
interface SessionStorage {

    /**
     * The stored server URL, or `""` before the first login.
     *
     * Not `suspend`: Ktor's `defaultRequest` block reads it per request and cannot suspend. The
     * implementation keeps the value cached.
     */
    val serverUrl: String

    /**
     * `true` while a token and credentials are stored and decrypt. The app's startup branch reads
     * it: a login swaps the app to the logged-in side, and clearing the session swaps it back.
     */
    val isLoggedIn: Flow<Boolean>

    suspend fun saveServerUrl(url: String)

    /** `null` before the first login, or when a stored value no longer decrypts. */
    suspend fun getCredentials(): Credentials?

    suspend fun saveCredentials(credentials: Credentials)

    /** `null` before the first login, or when the stored value no longer decrypts. */
    suspend fun getToken(): String?

    suspend fun saveToken(token: String)

    /**
     * Removes the credentials and the JWT, so [isLoggedIn] turns `false`. The server URL stays: the
     * login screen shows it again.
     */
    suspend fun clearSession()
}

data class Credentials(val username: String, val password: String) {
    /** The password stays out of logs and crash reports. */
    override fun toString(): String = "Credentials(username=$username, password=REDACTED)"
}
