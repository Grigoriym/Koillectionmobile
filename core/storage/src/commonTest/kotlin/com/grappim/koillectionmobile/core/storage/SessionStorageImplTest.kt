package com.grappim.koillectionmobile.core.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import com.grappim.kit.testing.FakeSecretCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `logged in needs both the credentials and the token`() = runTest {
        assertFalse(storage.isLoggedIn.first())

        storage.saveCredentials(Credentials(username = "tester", password = "hunter2"))
        assertFalse(storage.isLoggedIn.first())

        storage.saveToken("jwt-1")
        assertTrue(storage.isLoggedIn.first())
    }

    @Test
    fun `a session that no longer decrypts is not logged in`() = runTest {
        storage.saveCredentials(Credentials(username = "tester", password = "hunter2"))
        storage.saveToken("jwt-1")
        cipher.decryptResult = { null }

        assertFalse(storage.isLoggedIn.first())
    }

    @Test
    fun `clearing the session logs out and keeps the server URL`() = runTest {
        storage.saveServerUrl("https://koi.example/")
        storage.saveCredentials(Credentials(username = "tester", password = "hunter2"))
        storage.saveToken("jwt-1")

        storage.clearSession()

        assertFalse(storage.isLoggedIn.first())
        assertNull(storage.getCredentials())
        assertNull(storage.getToken())
        assertEquals("https://koi.example/", storage.serverUrl)
        assertEquals("https://koi.example/", SessionStorageImpl(dataStore, cipher).serverUrl)
    }

    @Test
    fun `the password is not in the string form of the credentials`() {
        assertEquals(
            "Credentials(username=tester, password=REDACTED)",
            Credentials(username = "tester", password = "hunter2").toString()
        )
    }
}
