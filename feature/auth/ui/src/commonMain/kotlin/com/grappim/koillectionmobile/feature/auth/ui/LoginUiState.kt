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
    val onLogInClick: () -> Unit = {}
) {
    val canLogIn: Boolean
        get() = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    /** The password stays out of logs and crash reports. */
    override fun toString(): String = "LoginUiState(serverUrl=$serverUrl, username=$username, password=REDACTED, " +
        "isLoading=$isLoading, error=$error)"
}
