package com.grappim.koillectionmobile.feature.collections.data

import com.grappim.koillectionmobile.core.storage.Credentials
import com.grappim.koillectionmobile.core.storage.SessionStorage

internal class FakeSessionStorage(
    override var serverUrl: String = "",
    var credentials: Credentials? = null,
    var token: String? = null
) : SessionStorage {

    override suspend fun saveServerUrl(url: String) {
        serverUrl = url
    }

    override suspend fun getCredentials(): Credentials? = credentials

    override suspend fun saveCredentials(credentials: Credentials) {
        this.credentials = credentials
    }

    override suspend fun getToken(): String? = token

    override suspend fun saveToken(token: String) {
        this.token = token
    }
}
