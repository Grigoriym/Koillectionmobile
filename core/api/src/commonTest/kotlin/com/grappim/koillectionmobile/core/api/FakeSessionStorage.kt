package com.grappim.koillectionmobile.core.api

import com.grappim.koillectionmobile.core.storage.Credentials
import com.grappim.koillectionmobile.core.storage.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeSessionStorage(
    override var serverUrl: String = "https://koi.example/sub",
    var credentials: Credentials? = null,
    var token: String? = null
) : SessionStorage {

    override val isLoggedIn: Flow<Boolean>
        get() = flowOf(credentials != null && token != null)

    val savedTokens = mutableListOf<String>()

    override suspend fun saveServerUrl(url: String) {
        serverUrl = url
    }

    override suspend fun getCredentials(): Credentials? = credentials

    override suspend fun saveCredentials(credentials: Credentials) {
        this.credentials = credentials
    }

    override suspend fun getToken(): String? = token

    override suspend fun saveToken(token: String) {
        savedTokens += token
        this.token = token
    }
}
