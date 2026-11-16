package com.grappim.koillectionmobile.composeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.koillectionmobile.feature.auth.ui.LoginScreen
import com.grappim.koillectionmobile.feature.settings.ui.SettingsScreen

/**
 * The logged-out side. Login is not a route and has no back stack, so whether Settings shows is
 * state here (wallosmobile's `LoginBranch`). The logged-in side opens the same screen as
 * `SettingsRoute`.
 */
@Composable
internal fun LoginBranch(modifier: Modifier = Modifier) {
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    if (isSettingsOpen) {
        val close = { isSettingsOpen = false }
        SettingsScreen(onBack = close, modifier = modifier)
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = close
        )
    } else {
        LoginScreen(onSettingsClick = { isSettingsOpen = true }, modifier = modifier)
    }
}
