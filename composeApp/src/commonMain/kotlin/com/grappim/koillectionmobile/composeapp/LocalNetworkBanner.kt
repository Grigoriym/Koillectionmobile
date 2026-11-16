package com.grappim.koillectionmobile.composeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.koillectionmobile.core.storage.SessionStorage
import com.grappim.koillectionmobile.feature.auth.ui.permission.rememberIsLocalNetworkBlocked
import com.grappim.koillectionmobile.feature.auth.ui.permission.rememberLocalNetworkPermissionRequest
import com.grappim.koillectionmobile.feature.auth.ui.permission.rememberOpenAppSettings
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.local_network_banner_message
import com.grappim.koillectionmobile.strings.generated.resources.local_network_banner_open_settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Puts a banner above [content] while the stored server is on the local network and the app may not
 * reach it. A blocked server fails every screen the same way, and each screen would only report it
 * as unreachable. A session from before the permission existed never passes through Log in, so this
 * also asks once per run (wallosmobile's `LocalNetworkBanner`, its plan §4.5c).
 *
 * The banner takes the top inset, so the screens below it do not add a second status bar gap.
 */
@Composable
internal fun LocalNetworkBanner(
    modifier: Modifier = Modifier,
    sessionStorage: SessionStorage = koinInject(),
    content: @Composable () -> Unit
) {
    val localNetworkPermission = rememberLocalNetworkPermissionRequest()
    val openAppSettings = rememberOpenAppSettings()
    // The first read of `serverUrl` blocks on DataStore.
    val serverUrl by produceState("") {
        value = withContext(Dispatchers.Default) { sessionStorage.serverUrl }
    }
    var hasAsked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(serverUrl) {
        if (serverUrl.isNotEmpty() && !hasAsked) {
            hasAsked = true
            localNetworkPermission.request(serverUrl = serverUrl, onAllowed = {}, onDenied = {})
        }
    }

    val isBlocked = rememberIsLocalNetworkBlocked(serverUrl)
    Column(modifier = modifier.fillMaxSize()) {
        if (isBlocked) {
            LocalNetworkBannerContent(onOpenSettingsClick = openAppSettings)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (isBlocked) Modifier.consumeWindowInsets(bannerInsets) else Modifier)
        ) {
            content()
        }
    }
}

@Composable
private fun LocalNetworkBannerContent(onOpenSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(bannerInsets)
                .padding(start = BANNER_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BANNER_PADDING)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(RString.local_network_banner_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onOpenSettingsClick) {
                Text(stringResource(RString.local_network_banner_open_settings))
            }
        }
    }
}

private val BANNER_PADDING = 16.dp

private val bannerInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
