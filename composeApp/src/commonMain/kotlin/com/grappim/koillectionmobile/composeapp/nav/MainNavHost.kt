package com.grappim.koillectionmobile.composeapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListScreen
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionScreen
import com.grappim.koillectionmobile.feature.items.ui.AddItemRoute
import com.grappim.koillectionmobile.feature.items.ui.AddItemScreen
import com.grappim.koillectionmobile.feature.items.ui.ItemRoute
import com.grappim.koillectionmobile.feature.items.ui.ItemScreen
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsScreen

/**
 * The logged-in side: one back stack that starts at the collection list.
 *
 * Each entry has its own `ViewModelStore`. Koin keys a ViewModel by its class only, so without the
 * decorator every entry of one type would share the first one's ViewModel (a second item would
 * show the first item), and a log out would not clear the logged-in screens' state.
 */
@Composable
internal fun MainNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, CollectionListRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<CollectionListRoute> {
                CollectionListScreen(
                    onCollectionClick = { id -> backStack.add(CollectionRoute(id)) },
                    onSettingsClick = { backStack.add(SettingsRoute) }
                )
            }
            entry<CollectionRoute> { route ->
                CollectionScreen(
                    collectionId = route.collectionId,
                    onCollectionClick = { id -> backStack.add(CollectionRoute(id)) },
                    onItemClick = { id -> backStack.add(ItemRoute(id)) },
                    onAddItemClick = { backStack.add(AddItemRoute(route.collectionId)) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<ItemRoute> { route ->
                ItemScreen(
                    itemId = route.itemId,
                    onBack = { backStack.removeLastOrNull() },
                    // The item is gone; its collection reads its list again (ItemListChanges).
                    onClose = { backStack.remove(route) }
                )
            }
            entry<SettingsRoute> {
                SettingsScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<AddItemRoute> { route ->
                AddItemScreen(
                    collectionId = route.collectionId,
                    onBack = { backStack.removeLastOrNull() },
                    // The new item replaces the form, so back goes to the collection.
                    onOpenItem = { id ->
                        backStack.removeLastOrNull()
                        backStack.add(ItemRoute(id))
                    }
                )
            }
        }
    )
}
