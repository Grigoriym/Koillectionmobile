package com.grappim.koillectionmobile.feature.auth.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

private class PendingResult(val onAllowed: () -> Unit, val onDenied: () -> Unit)

@Composable
actual fun rememberLocalNetworkPermissionRequest(): LocalNetworkPermissionRequest {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { arrayOfNulls<PendingResult>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val result = pending[0]
        pending[0] = null
        if (isGranted) result?.onAllowed?.invoke() else result?.onDenied?.invoke()
    }

    return remember(context, launcher) {
        LocalNetworkPermissionRequest { serverUrl, onAllowed, onDenied ->
            scope.launch {
                if (context.needsLocalNetworkPermission() && resolvesToLocalNetwork(serverUrl)) {
                    pending[0] = PendingResult(onAllowed, onDenied)
                    launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                } else {
                    onAllowed()
                }
            }
        }
    }
}

@Composable
actual fun rememberIsLocalNetworkBlocked(serverUrl: String): Boolean {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBlocked by remember { mutableStateOf(false) }

    LifecycleResumeEffect(serverUrl) {
        val check = scope.launch {
            isBlocked = context.needsLocalNetworkPermission() && resolvesToLocalNetwork(serverUrl)
        }
        onPauseOrDispose { check.cancel() }
    }
    return isBlocked
}

@Composable
actual fun rememberOpenAppSettings(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        }
    }
}

private fun Context.needsLocalNetworkPermission(): Boolean = Build.VERSION.SDK_INT >= FIRST_ENFORCING_SDK &&
    checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED

/**
 * Resolves the name rather than reading it: a homelab hostname on a public-looking domain can point
 * at a private address. A name that does not resolve is not local: the request that follows
 * reports the failure.
 */
private suspend fun resolvesToLocalNetwork(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
    val host = Uri.parse(serverUrl.trim()).host ?: return@withContext false
    try {
        InetAddress.getAllByName(host).any { isLocalNetworkAddress(it.address) }
    } catch (e: UnknownHostException) {
        logcat(priority = LogPriority.WARN, throwable = e) { "Could not resolve the server's host" }
        false
    }
}

private const val FIRST_ENFORCING_SDK = 37
