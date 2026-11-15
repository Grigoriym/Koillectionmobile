package com.grappim.koillectionmobile.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grappim.kit.uikit.KitTheme
import com.grappim.koillectionmobile.composeapp.nav.MainNavHost
import com.grappim.koillectionmobile.core.storage.SessionStorage
import com.grappim.koillectionmobile.feature.auth.ui.LoginScreen
import org.koin.compose.koinInject

/**
 * The app's single composable entry point, so `MainActivity` knows about no other module.
 *
 * The startup branch is state, not navigation: [SessionStorage.isLoggedIn] is the one source of
 * truth. A login and a log-out both swap the whole tree, and neither side calls the other. Login
 * is not a `NavKey`, so it never enters a back stack (wallosmobile's `WallosAppContent`).
 */
@Composable
fun KoillectionAppContent(modifier: Modifier = Modifier, sessionStorage: SessionStorage = koinInject()) {
    /*
     * The saved seed is load-bearing. `isLoggedIn` is a DataStore flow with no value for the first
     * frame. Without the seed, `MainNavHost` composes in a later pass, and `rememberNavBackStack`
     * reads its restored state only in the first pass: after process death the back stack is lost.
     */
    var lastKnownLoggedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val isLoggedIn by sessionStorage.isLoggedIn.collectAsState(initial = lastKnownLoggedIn)

    LaunchedEffect(isLoggedIn) {
        lastKnownLoggedIn = isLoggedIn
    }

    KitTheme(
        lightColorScheme = lightColorScheme(),
        darkColorScheme = darkColorScheme(),
        typography = Typography()
    ) {
        when (isLoggedIn) {
            // Only on a first launch: nothing saved, and DataStore has not answered yet. It lasts
            // a frame, so it is blank and not a progress indicator.
            null -> Box(modifier = modifier.fillMaxSize())

            true -> MainNavHost(modifier = modifier)

            false -> LoginScreen(modifier = modifier)
        }
    }
}
