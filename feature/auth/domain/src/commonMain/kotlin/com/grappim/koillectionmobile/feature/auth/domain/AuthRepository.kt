package com.grappim.koillectionmobile.feature.auth.domain

interface AuthRepository {

    /**
     * Logs in to the Koillection server at [serverUrl] (the instance root, with a subpath if it has
     * one). On [LoginResult.LoggedIn] the URL, the credentials and the JWT are stored. On any other
     * result the stored session stays as it was.
     */
    suspend fun logIn(serverUrl: String, username: String, password: String): LoginResult
}
