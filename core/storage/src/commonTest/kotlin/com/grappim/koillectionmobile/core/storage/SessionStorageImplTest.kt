package com.grappim.koillectionmobile.core.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import com.grappim.kit.testing.FakeSecretCipher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionStorageImplTest {

    private val dataStore = FakePreferencesDataStore()
    private val cipher = FakeSecretCipher()
    private val storage = SessionStorageImpl(dataStore, cipher)

    @Test
    fun `nothing is stored before the first login`() = runTest {
        assertEquals("", storage.serverUrl)
        assertNull(storage.getCredentials())
        assertNull(storage.getToken())
    }

    @Test
    fun `saved values read back`() = runTest {
        storage.saveServerUrl("https://koi.example/")
        storage.saveCredentials(Credentials(username = "tester", password = "hunter2"))
        storage.saveToken("jwt-1")

        assertEquals("https://koi.example/", storage.serverUrl)
        assertEquals(Credentials(username = "tester", password = "hunter2"), storage.getCredentials())
        assertEquals("jwt-1", storage.getToken())
    }

    @Test
    fun `the server URL survives a new instance on the same store`() = runTest {
        storage.saveServerUrl("https://koi.example/")

        assertEquals("https://koi.example/", SessionStorageImpl(dataStore, cipher).serverUrl)
    }

    @Test
    fun `the password and the token are stored encrypted`() = runTest {
        storage.saveCredentials(Credentials(username = "tester", password = "hunter2"))
        storage.saveToken("jwt-1")

        assertEquals("ENC:hunter2", dataStore.current[stringPreferencesKey("password")])
        assertEquals("ENC:jwt-1", dataStore.current[stringPreferencesKey("token")])
    }

    @Test
    fun `values that no longer decrypt read as absent`() = runTest {
        storage.saveCredentials(Credentials(username = "tester", password = "hunter2"))
        storage.saveToken("jwt-1")
        cipher.decryptResult = { null }

        assertNull(storage.getCredentials())
        assertNull(storage.getToken())
    }

    @Test
    fun `the password is not in the string form of the credentials`() {
        assertEquals(
            "Credentials(username=tester, password=REDACTED)",
            Credentials(username = "tester", password = "hunter2").toString()
        )
    }
}
