package com.grappim.koillectionmobile.composeapp

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.grappim.kit.navigation.NavigationState
import com.grappim.kit.navigation.rememberNavigationState
import com.grappim.koillectionmobile.composeapp.nav.DRAWER_NAV_ITEMS
import com.grappim.koillectionmobile.composeapp.nav.DrawerConfig
import com.grappim.koillectionmobile.composeapp.nav.DrawerDestination
import com.grappim.koillectionmobile.composeapp.nav.RouteConfig
import com.grappim.koillectionmobile.composeapp.nav.RouteConfigProvider
import com.grappim.koillectionmobile.composeapp.nav.navSavedStateConfiguration
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import kotlinx.coroutines.CoroutineScope

/** The collection list is the start section. The app has no setting to change it. */
@Composable
fun rememberMainAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
): MainAppState {
    val navigationState = rememberNavigationState(
        startKey = CollectionListRoute,
        topLevelKeys = DRAWER_NAV_ITEMS,
        configuration = navSavedStateConfiguration
    )

    return remember(navigationState, coroutineScope, drawerState) {
        MainAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
            drawerState = drawerState
        )
    }
}

/**
 * What the shell knows about where it is: the back stacks, the drawer, and the current route's
 * configuration (wallosmobile's `MainAppState`).
 */
@Stable
class MainAppState(
    val navigationState: NavigationState,
    val coroutineScope: CoroutineScope,
    val drawerState: DrawerState
) {

    val currentRouteConfig: RouteConfig by derivedStateOf {
        RouteConfigProvider.getConfig(navigationState.currentKey)
    }

    val drawerGesturesEnabled: Boolean by derivedStateOf {
        currentRouteConfig.drawerConfig == DrawerConfig.Enabled
    }

    /** The highlighted drawer item: the current section, not the screen on top of it. */
    val currentDrawerDestination: DrawerDestination? by derivedStateOf {
        DrawerDestination.entries.find { it.route == navigationState.currentTopLevelKey }
    }

    /**
     * On the root of a section other than the start section. `NavDisplay` handles no back there
     * (one entry), so the shell sends back to the start section itself.
     */
    val isAtOtherSectionRoot: Boolean by derivedStateOf {
        navigationState.currentKey == navigationState.currentTopLevelKey &&
            navigationState.currentTopLevelKey::class != navigationState.startKey::class
    }
}
