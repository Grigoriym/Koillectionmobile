package com.grappim.koillectionmobile.feature.auth.ui.permission

import androidx.compose.runtime.Composable

// Only Android 17 gates the local network behind a runtime permission.

@Composable
actual fun rememberLocalNetworkPermissionRequest(): LocalNetworkPermissionRequest =
    LocalNetworkPermissionRequest { _, onAllowed, _ -> onAllowed() }

@Composable
actual fun rememberIsLocalNetworkBlocked(serverUrl: String): Boolean = false

@Composable
actual fun rememberOpenAppSettings(): () -> Unit = {}
