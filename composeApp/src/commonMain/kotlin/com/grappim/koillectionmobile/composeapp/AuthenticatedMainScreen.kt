package com.grappim.koillectionmobile.composeapp

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.navigation.Navigator
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.drawer.DrawerWidget
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBar
import com.grappim.kit.uikit.widgets.topbar.TopBarController
import com.grappim.koillectionmobile.composeapp.nav.DrawerDestination
import com.grappim.koillectionmobile.composeapp.nav.DrawerItemsBuilder
import com.grappim.koillectionmobile.composeapp.nav.MainNavHost
import com.grappim.koillectionmobile.composeapp.nav.goBackOrToStart
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.app_name
import com.grappim.koillectionmobile.strings.generated.resources.top_bar_back
import com.grappim.koillectionmobile.strings.generated.resources.top_bar_menu
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The shell every logged-in screen renders inside: the drawer, the top bar and the nav display
 * (wallosmobile's `AuthenticatedMainScreen`).
 */
@Composable
fun AuthenticatedMainScreen(
    appState: MainAppState,
    modifier: Modifier = Modifier,
    drawerItemsBuilder: DrawerItemsBuilder = koinInject(),
    drawerViewModel: DrawerViewModel = koinViewModel()
) {
    val navigator = remember(appState) { Navigator(appState.navigationState) }
    val topBarController = remember { TopBarController() }
    val drawerItems = remember(drawerItemsBuilder) { drawerItemsBuilder.build() }

    CompositionLocalProvider(LocalTopBarConfig provides topBarController) {
        DrawerWidget(
            modifier = modifier,
            drawerItems = drawerItems,
            currentTopLevelDestination = appState.currentDrawerDestination,
            drawerState = appState.drawerState,
            headerTitle = NativeText.Resource(RString.app_name),
            onDrawerItemClick = { destination: DrawerDestination ->
                appState.coroutineScope.launch {
                    appState.drawerState.close()
                }
                val route = destination.route
                if (route == null) drawerViewModel.logOut() else navigator.navigate(route)
            },
            gesturesEnabled = appState.drawerGesturesEnabled
        ) {
            MainScaffold(appState = appState, navigator = navigator, topBarController = topBarController)
        }
    }
}

@Composable
private fun MainScaffold(
    appState: MainAppState,
    navigator: Navigator,
    topBarController: TopBarController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopBar(
                isVisible = appState.currentRouteConfig.hasShellTopBar &&
                    topBarController.config.navigationIcon !is NavigationIconConfig.None,
                drawerState = appState.drawerState,
                topBarConfig = topBarController.config,
                defaultGoBack = { navigator.goBackOrToStart() },
                backContentDescription = stringResource(RString.top_bar_back),
                menuContentDescription = stringResource(RString.top_bar_menu)
            )
        }
    ) { innerPadding ->
        // Consumed, so a screen that still draws its own top bar adds no second status bar gap.
        LocalNetworkBanner(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
        ) {
            MainNavHost(navigationState = appState.navigationState, navigator = navigator)
        }

        // Back at the root of Tags, Templates, Wishlists, Albums or Settings goes to Collections.
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = appState.isAtOtherSectionRoot,
            onBackCompleted = { navigator.goBackOrToStart() }
        )

        /*
         * Composed last on purpose: of several enabled back handlers, the one composed last wins.
         * Composed earlier, back moves the stack with the drawer still open. `isAnimationRunning`
         * covers a drawer that looks open but is not open yet.
         */
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = appState.drawerState.isOpen || appState.drawerState.isAnimationRunning,
            onBackCompleted = {
                appState.coroutineScope.launch {
                    appState.drawerState.close()
                }
            }
        )
    }
}
