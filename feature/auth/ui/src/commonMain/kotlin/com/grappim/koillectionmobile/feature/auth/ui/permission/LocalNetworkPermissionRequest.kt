package com.grappim.koillectionmobile.feature.auth.ui.permission

import androidx.compose.runtime.Composable

/**
 * Asks for local network access when a server needs it, before anything is sent to that server
 * (Android 17, `ACCESS_LOCAL_NETWORK`). [request] calls `onAllowed` when the app may go ahead, which
 * includes every server that is not on the local network. It calls `onDenied` only when that
 * server is certain to be blocked. Ported from wallosmobile `uikit` `permission/` (its plan §4.5c).
 */
fun interface LocalNetworkPermissionRequest {
    fun request(serverUrl: String, onAllowed: () -> Unit, onDenied: () -> Unit)
}

@Composable
expect fun rememberLocalNetworkPermissionRequest(): LocalNetworkPermissionRequest

/** Whether [serverUrl] is on the local network with the permission off. It is checked again on every resume. */
@Composable
expect fun rememberIsLocalNetworkBlocked(serverUrl: String): Boolean

/** Opens the system page for this app: the way back once the permission dialog no longer shows. */
@Composable
expect fun rememberOpenAppSettings(): () -> Unit
