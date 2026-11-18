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
import com.grappim.koillectionmobile.feature.collections.ui.EditCollectionRoute
import com.grappim.koillectionmobile.feature.collections.ui.EditCollectionScreen
import com.grappim.koillectionmobile.feature.collections.ui.ItemSearchRoute
import com.grappim.koillectionmobile.feature.collections.ui.ItemSearchScreen
import com.grappim.koillectionmobile.feature.collections.ui.NewCollectionRoute
import com.grappim.koillectionmobile.feature.collections.ui.NewCollectionScreen
import com.grappim.koillectionmobile.feature.items.ui.AddItemRoute
import com.grappim.koillectionmobile.feature.items.ui.AddItemScreen
import com.grappim.koillectionmobile.feature.items.ui.EditItemRoute
import com.grappim.koillectionmobile.feature.items.ui.EditItemScreen
import com.grappim.koillectionmobile.feature.items.ui.ItemRoute
import com.grappim.koillectionmobile.feature.items.ui.ItemScreen
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsScreen
import com.grappim.koillectionmobile.feature.tags.ui.TagItemsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagItemsScreen
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListScreen
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListFormRoute
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListFormScreen
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListListRoute
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListListScreen
import com.grappim.koillectionmobile.feature.templates.ui.TemplateFormRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateFormScreen
import com.grappim.koillectionmobile.feature.templates.ui.TemplateListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateListScreen
import com.grappim.koillectionmobile.feature.templates.ui.TemplateRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateScreen

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
                    onSettingsClick = { backStack.add(SettingsRoute) },
                    onNewCollectionClick = { backStack.add(NewCollectionRoute()) },
                    onSearchClick = { backStack.add(ItemSearchRoute) },
                    onTagsClick = { backStack.add(TagListRoute) },
                    onTemplatesClick = { backStack.add(TemplateListRoute) }
                )
            }
            entry<ItemSearchRoute> {
                ItemSearchScreen(
                    onItemClick = { id -> backStack.add(ItemRoute(id)) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<TagListRoute> {
                TagListScreen(
                    onTagClick = { tag -> backStack.add(TagItemsRoute(tagId = tag.id, label = tag.label)) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<TagItemsRoute> { route ->
                TagItemsScreen(
                    tagId = route.tagId,
                    label = route.label,
                    onItemClick = { id -> backStack.add(ItemRoute(id)) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<TemplateListRoute> {
                TemplateListScreen(
                    onTemplateClick = { template ->
                        backStack.add(TemplateRoute(templateId = template.id, name = template.name))
                    },
                    onNewTemplateClick = { backStack.add(TemplateFormRoute()) },
                    onChoiceListsClick = { backStack.add(ChoiceListListRoute) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<TemplateRoute> { route ->
                TemplateScreen(
                    templateId = route.templateId,
                    name = route.name,
                    onBack = { backStack.removeLastOrNull() },
                    onEdit = { backStack.add(TemplateFormRoute(route.templateId)) },
                    // The template is gone; the list reads its list again (TemplateChanges).
                    onClose = { backStack.remove(route) }
                )
            }
            entry<TemplateFormRoute> { route ->
                TemplateFormScreen(
                    templateId = route.templateId,
                    onBack = { backStack.removeLastOrNull() },
                    // An edit goes back to the template screen, which reads it again (TemplateChanges).
                    // A new template replaces the form, so back goes to the list.
                    onSave = { id ->
                        backStack.remove(route)
                        if (route.templateId == null) backStack.add(TemplateRoute(templateId = id, name = ""))
                    },
                    onChoiceListsClick = { backStack.add(ChoiceListListRoute) }
                )
            }
            entry<ChoiceListListRoute> {
                ChoiceListListScreen(
                    onChoiceListClick = { id -> backStack.add(ChoiceListFormRoute(id)) },
                    onNewChoiceListClick = { backStack.add(ChoiceListFormRoute()) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<ChoiceListFormRoute> { route ->
                ChoiceListFormScreen(
                    choiceListId = route.choiceListId,
                    onBack = { backStack.removeLastOrNull() },
                    // The screens below read the choice lists again (ChoiceListChanges).
                    onDone = { backStack.remove(route) }
                )
            }
            entry<CollectionRoute> { route ->
                CollectionScreen(
                    collectionId = route.collectionId,
                    onCollectionClick = { id -> backStack.add(CollectionRoute(id)) },
                    onItemClick = { id -> backStack.add(ItemRoute(id)) },
                    onAddItemClick = { backStack.add(AddItemRoute(route.collectionId)) },
                    onNewCollectionClick = { backStack.add(NewCollectionRoute(route.collectionId)) },
                    onEditClick = { backStack.add(EditCollectionRoute(route.collectionId)) },
                    onBack = { backStack.removeLastOrNull() },
                    // The collection is gone; the list or the parent reads it again (CollectionListChanges).
                    onClose = { backStack.remove(route) }
                )
            }
            entry<ItemRoute> { route ->
                ItemScreen(
                    itemId = route.itemId,
                    onBack = { backStack.removeLastOrNull() },
                    // The item is gone; its collection reads its list again (ItemListChanges).
                    onClose = { backStack.remove(route) },
                    onEdit = { backStack.add(EditItemRoute(route.itemId)) }
                )
            }
            entry<EditItemRoute> { route ->
                EditItemScreen(itemId = route.itemId, onBack = { backStack.removeLastOrNull() })
            }
            entry<NewCollectionRoute> { route ->
                NewCollectionScreen(
                    parentId = route.parentId,
                    onBack = { backStack.removeLastOrNull() },
                    // The new collection replaces the form, so back goes to the list or the parent.
                    onOpenCollection = { id ->
                        backStack.remove(route)
                        backStack.add(CollectionRoute(id))
                    }
                )
            }
            entry<EditCollectionRoute> { route ->
                EditCollectionScreen(
                    collectionId = route.collectionId,
                    onBack = { backStack.removeLastOrNull() },
                    // The collection screen and the list read the collection again (CollectionChanges).
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
