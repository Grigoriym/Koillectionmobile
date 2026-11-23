package com.grappim.koillectionmobile.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBar
import com.grappim.kit.uikit.widgets.topbar.TopBarController
import com.grappim.koillectionmobile.feature.auth.ui.LoginScreen
import com.grappim.koillectionmobile.feature.settings.ui.SettingsScreen
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.top_bar_back
import com.grappim.koillectionmobile.strings.generated.resources.top_bar_menu
import org.jetbrains.compose.resources.stringResource

/**
 * The logged-out side. Login is not a route and has no back stack, so whether Settings shows is
 * state here (wallosmobile's `LoginBranch`). The logged-in side opens the same screen as
 * `SettingsRoute`.
 */
@Composable
internal fun LoginBranch(modifier: Modifier = Modifier) {
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    if (isSettingsOpen) {
        LoginSettings(onBack = { isSettingsOpen = false }, modifier = modifier)
    } else {
        LoginScreen(onSettingsClick = { isSettingsOpen = true }, modifier = modifier)
    }
}

/**
 * There is no shell here, so this gives Settings the top bar it reads from the shell
 * (wallosmobile's `LoginCustomHeaders`).
 */
@Composable
private fun LoginSettings(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val topBarController = remember { TopBarController() }

    CompositionLocalProvider(LocalTopBarConfig provides topBarController) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopBar(
                    isVisible = true,
                    drawerState = rememberDrawerState(DrawerValue.Closed),
                    topBarConfig = topBarController.config,
                    defaultGoBack = onBack,
                    backContentDescription = stringResource(RString.top_bar_back),
                    menuContentDescription = stringResource(RString.top_bar_menu)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize()
            ) {
                SettingsScreen(navigationIcon = NavigationIconConfig.Back(onBackClick = onBack))
            }
        }

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = onBack
        )
    }
}
