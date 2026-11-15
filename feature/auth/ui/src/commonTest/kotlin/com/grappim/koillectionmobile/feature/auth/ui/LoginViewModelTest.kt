package com.grappim.koillectionmobile.feature.auth.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.login_error_http
import com.grappim.koillectionmobile.strings.generated.resources.login_error_invalid_credentials
import com.grappim.koillectionmobile.strings.generated.resources.login_error_invalid_server_url
import com.grappim.koillectionmobile.strings.generated.resources.login_error_not_koillection
import com.grappim.koillectionmobile.strings.generated.resources.login_error_server_jwt
import com.grappim.koillectionmobile.strings.generated.resources.login_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.login_error_unreachable
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {

    private val repository = FakeAuthRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun filledViewModel() = LoginViewModel(repository).apply {
        with(uiState.value) {
            onServerUrlChange(SERVER_URL)
            onUsernameChange(USERNAME)
            onPasswordChange(PASSWORD)
        }
    }

    private fun messageFor(result: LoginResult): NativeText {
        repository.result = CompletableDeferred(result)
        val sut = filledViewModel()
        sut.uiState.value.onLogInClick()
        assertFalse(sut.uiState.value.isLoading)
        return sut.uiState.value.error
    }

    @Test
    fun `log in is disabled until every field is filled`() {
        val sut = LoginViewModel(repository)
        assertFalse(sut.uiState.value.canLogIn)

        sut.uiState.value.onServerUrlChange(SERVER_URL)
        sut.uiState.value.onUsernameChange(USERNAME)
        sut.uiState.value.onPasswordChange("   ")
        assertFalse(sut.uiState.value.canLogIn)

        sut.uiState.value.onPasswordChange(PASSWORD)
        assertTrue(sut.uiState.value.canLogIn)
    }

    @Test
    fun `a click with an empty field sends nothing`() {
        val sut = LoginViewModel(repository)
        sut.uiState.value.onServerUrlChange(SERVER_URL)

        sut.uiState.value.onLogInClick()

        assertNull(repository.call)
    }

    @Test
    fun `log in passes the typed fields to the repository`() {
        filledViewModel().uiState.value.onLogInClick()

        assertEquals(Triple(SERVER_URL, USERNAME, PASSWORD), repository.call)
    }

    @Test
    fun `the screen is loading while the request runs`() {
        val sut = filledViewModel()

        sut.uiState.value.onLogInClick()
        assertTrue(sut.uiState.value.isLoading)
        assertFalse(sut.uiState.value.canLogIn)

        repository.result.complete(LoginResult.InvalidCredentials)
        assertFalse(sut.uiState.value.isLoading)
    }

    @Test
    fun `LoggedIn shows no error and drops the username and the password`() {
        repository.result = CompletableDeferred(LoginResult.LoggedIn)
        val sut = filledViewModel()

        sut.uiState.value.onLogInClick()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals("", sut.uiState.value.username)
        assertEquals("", sut.uiState.value.password)
        assertEquals(SERVER_URL, sut.uiState.value.serverUrl)
    }

    @Test
    fun `the server URL of the last login is filled in`() {
        repository.storedServerUrl = SERVER_URL

        val sut = LoginViewModel(repository)

        assertEquals(SERVER_URL, sut.uiState.value.serverUrl)
        assertEquals("", sut.uiState.value.username)
        assertEquals("", sut.uiState.value.password)
    }

    @Test
    fun `the server URL is empty before the first login`() {
        assertEquals("", LoginViewModel(repository).uiState.value.serverUrl)
    }

    @Test
    fun `InvalidServerUrl has its message`() {
        assertEquals(
            NativeText.Resource(RString.login_error_invalid_server_url),
            messageFor(LoginResult.InvalidServerUrl)
        )
    }

    @Test
    fun `Unreachable has its message`() {
        assertEquals(NativeText.Resource(RString.login_error_unreachable), messageFor(LoginResult.Unreachable))
    }

    @Test
    fun `NotKoillection has its message`() {
        assertEquals(NativeText.Resource(RString.login_error_not_koillection), messageFor(LoginResult.NotKoillection))
    }

    @Test
    fun `InvalidCredentials has its message`() {
        assertEquals(
            NativeText.Resource(RString.login_error_invalid_credentials),
            messageFor(LoginResult.InvalidCredentials)
        )
    }

    @Test
    fun `ServerJwtFailure has the JWT passphrase message`() {
        assertEquals(NativeText.Resource(RString.login_error_server_jwt), messageFor(LoginResult.ServerJwtFailure))
    }

    @Test
    fun `Failed with an HTTP status names the status`() {
        assertEquals(
            NativeText.Arguments(RString.login_error_http, listOf(502)),
            messageFor(LoginResult.Failed(KoillectionError.Server(status = 502, message = "Bad Gateway")))
        )
        assertEquals(
            NativeText.Arguments(RString.login_error_http, listOf(403)),
            messageFor(LoginResult.Failed(KoillectionError.Http(status = 403, message = null)))
        )
    }

    @Test
    fun `Failed without a status has the unexpected message`() {
        assertEquals(
            NativeText.Resource(RString.login_error_unexpected),
            messageFor(LoginResult.Failed(KoillectionError.Network(IllegalStateException("connection reset"))))
        )
    }

    @Test
    fun `editing a field clears the error`() {
        repository.result = CompletableDeferred(LoginResult.InvalidCredentials)
        val sut = filledViewModel()
        sut.uiState.value.onLogInClick()
        assertTrue(sut.uiState.value.error.isNotEmpty())

        sut.uiState.value.onPasswordChange("another")

        assertTrue(sut.uiState.value.error.isEmpty())
    }

    private class FakeAuthRepository : AuthRepository {
        var result = CompletableDeferred<LoginResult>()
        var call: Triple<String, String, String>? = null
        var storedServerUrl = ""

        override suspend fun logIn(serverUrl: String, username: String, password: String): LoginResult {
            call = Triple(serverUrl, username, password)
            return result.await()
        }

        override suspend fun logOut() = error("not used")

        override fun storedServerUrl(): String = storedServerUrl
    }

    private companion object {
        const val SERVER_URL = "http://10.0.2.2:8290"
        const val USERNAME = "tester"
        const val PASSWORD = "hunter2"
    }
}
