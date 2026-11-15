package com.grappim.koillectionmobile.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The ViewModel does not navigate. On [LoginResult.LoggedIn] the repository has stored the session,
 * so `SessionStorage.isLoggedIn` turns `true` and the app shell swaps to the logged-in side.
 */
@KoinViewModel
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            onServerUrlChange = ::onServerUrlChange,
            onUsernameChange = ::onUsernameChange,
            onPasswordChange = ::onPasswordChange,
            onLogInClick = ::onLogInClick
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = NativeText.Empty) }
    }

    private fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = NativeText.Empty) }
    }

    private fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = NativeText.Empty) }
    }

    private fun onLogInClick() {
        val state = _uiState.value
        if (!state.canLogIn) return

        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        viewModelScope.launch {
            val result = authRepository.logIn(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password
            )
            _uiState.update {
                if (result == LoginResult.LoggedIn) {
                    // The password is stored now; the screen has no reason to keep it.
                    it.copy(isLoading = false, password = "")
                } else {
                    it.copy(isLoading = false, error = result.toMessage())
                }
            }
        }
    }
}

internal fun LoginResult.toMessage(): NativeText = when (this) {
    LoginResult.LoggedIn -> NativeText.Empty

    LoginResult.InvalidServerUrl -> NativeText.Resource(RString.login_error_invalid_server_url)

    LoginResult.Unreachable -> NativeText.Resource(RString.login_error_unreachable)

    LoginResult.NotKoillection -> NativeText.Resource(RString.login_error_not_koillection)

    LoginResult.InvalidCredentials -> NativeText.Resource(RString.login_error_invalid_credentials)

    LoginResult.ServerJwtFailure -> NativeText.Resource(RString.login_error_server_jwt)

    is LoginResult.Failed -> when (val error = error) {
        is KoillectionError.Server -> NativeText.Arguments(RString.login_error_http, listOf(error.status))
        is KoillectionError.Http -> NativeText.Arguments(RString.login_error_http, listOf(error.status))
        else -> NativeText.Resource(RString.login_error_unexpected)
    }
}
