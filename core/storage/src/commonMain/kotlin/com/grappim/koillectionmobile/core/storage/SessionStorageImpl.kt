package com.grappim.koillectionmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grappim.kit.storage.SecretCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.Volatile

/**
 * The password and the JWT go through [secretCipher]. The server URL and the username are stored
 * as plain text.
 */
class SessionStorageImpl(private val dataStore: DataStore<Preferences>, private val secretCipher: SecretCipher) :
    SessionStorage {

    @Volatile
    private var cachedServerUrl: String? = null

    /**
     * Blocks on the first read only, then serves the cache. Two threads that race here read the
     * same value, so the write is idempotent (wallosmobile's `ServerUrlStorageImpl`).
     */
    override val serverUrl: String
        get() = cachedServerUrl ?: runBlocking { read(KEY_SERVER_URL) }.orEmpty().also { cachedServerUrl = it }

    override suspend fun saveServerUrl(url: String) {
        dataStore.edit { it[KEY_SERVER_URL] = url }
        cachedServerUrl = url
    }

    override suspend fun getCredentials(): Credentials? {
        val prefs = dataStore.data.first()
        val username = prefs[KEY_USERNAME] ?: return null
        val password = prefs[KEY_PASSWORD]?.let(secretCipher::decrypt) ?: return null
        return Credentials(username = username, password = password)
    }

    override suspend fun saveCredentials(credentials: Credentials) {
        val encrypted = secretCipher.encrypt(credentials.password)
        dataStore.edit {
            it[KEY_USERNAME] = credentials.username
            it[KEY_PASSWORD] = encrypted
        }
    }

    override suspend fun getToken(): String? = read(KEY_TOKEN)?.let(secretCipher::decrypt)

    override suspend fun saveToken(token: String) {
        val encrypted = secretCipher.encrypt(token)
        dataStore.edit { it[KEY_TOKEN] = encrypted }
    }

    private suspend fun read(key: Preferences.Key<String>): String? = dataStore.data.first()[key]

    private companion object {
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_TOKEN = stringPreferencesKey("token")
    }
}
