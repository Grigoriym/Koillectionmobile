package com.grappim.koillectionmobile.feature.auth.ui

import com.grappim.kit.uikit.NativeText

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: NativeText = NativeText.Empty,
    val onServerUrlChange: (String) -> Unit = {},
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onLogInClick: () -> Unit = {},
    val onLocalNetworkDenied: () -> Unit = {}
) {
    val canLogIn: Boolean
        get() = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    /**
     * Advisory only: it never disables Log in, because a plain HTTP server on the LAN is a normal
     * case (and the test instance is one). Only a literal `http://` prefix counts: the app infers
     * no scheme anywhere.
     */
    val isCleartextWarningVisible: Boolean
        get() = serverUrl.trim().startsWith(CLEARTEXT_SCHEME, ignoreCase = true)

    /** The password stays out of logs and crash reports. */
    override fun toString(): String = "LoginUiState(serverUrl=$serverUrl, username=$username, password=REDACTED, " +
        "isLoading=$isLoading, error=$error)"

    private companion object {
        const val CLEARTEXT_SCHEME = "http://"
    }
}
