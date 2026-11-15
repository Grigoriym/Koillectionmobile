package com.grappim.koillectionmobile.composeapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListScreen
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionScreen
import com.grappim.koillectionmobile.feature.items.ui.ItemRoute
import com.grappim.koillectionmobile.feature.items.ui.ItemScreen

/** The logged-in side: one back stack that starts at the collection list. */
@Composable
internal fun MainNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, CollectionListRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<CollectionListRoute> {
                CollectionListScreen(onCollectionClick = { id -> backStack.add(CollectionRoute(id)) })
            }
            entry<CollectionRoute> { route ->
                CollectionScreen(
                    collectionId = route.collectionId,
                    onCollectionClick = { id -> backStack.add(CollectionRoute(id)) },
                    onItemClick = { id -> backStack.add(ItemRoute(id)) }
                )
            }
            entry<ItemRoute> { route -> ItemScreen(itemId = route.itemId) }
        }
    )
}
