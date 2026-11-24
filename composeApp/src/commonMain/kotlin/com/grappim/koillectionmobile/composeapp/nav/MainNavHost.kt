package com.grappim.koillectionmobile.composeapp.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.grappim.kit.navigation.NavigationState
import com.grappim.kit.navigation.Navigator
import com.grappim.kit.navigation.toEntries
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.koillectionmobile.feature.albums.ui.AlbumListRoute
import com.grappim.koillectionmobile.feature.albums.ui.AlbumListScreen
import com.grappim.koillectionmobile.feature.albums.ui.AlbumRoute
import com.grappim.koillectionmobile.feature.albums.ui.AlbumScreen
import com.grappim.koillectionmobile.feature.albums.ui.EditAlbumRoute
import com.grappim.koillectionmobile.feature.albums.ui.EditAlbumScreen
import com.grappim.koillectionmobile.feature.albums.ui.EditPhotoRoute
import com.grappim.koillectionmobile.feature.albums.ui.EditPhotoScreen
import com.grappim.koillectionmobile.feature.albums.ui.NewAlbumRoute
import com.grappim.koillectionmobile.feature.albums.ui.NewAlbumScreen
import com.grappim.koillectionmobile.feature.albums.ui.NewPhotoRoute
import com.grappim.koillectionmobile.feature.albums.ui.NewPhotoScreen
import com.grappim.koillectionmobile.feature.albums.ui.PhotoRoute
import com.grappim.koillectionmobile.feature.albums.ui.PhotoScreen
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
import com.grappim.koillectionmobile.feature.loans.ui.LoanFormRoute
import com.grappim.koillectionmobile.feature.loans.ui.LoanFormScreen
import com.grappim.koillectionmobile.feature.loans.ui.LoansSection
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
import com.grappim.koillectionmobile.feature.wishlists.ui.EditWishRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.EditWishScreen
import com.grappim.koillectionmobile.feature.wishlists.ui.EditWishlistRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.EditWishlistScreen
import com.grappim.koillectionmobile.feature.wishlists.ui.NewWishRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.NewWishScreen
import com.grappim.koillectionmobile.feature.wishlists.ui.NewWishlistRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.NewWishlistScreen
import com.grappim.koillectionmobile.feature.wishlists.ui.WishRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishScreen
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistListRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistListScreen
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistScreen

/**
 * The logged-in side: one back stack per drawer section, held in [navigationState]. Screens move
 * only through [navigator].
 *
 * Each entry has its own `ViewModelStore` (`toEntries` adds the decorator). Koin keys a ViewModel by
 * its class only, so without it every entry of one type would share the first one's ViewModel (a
 * second item would show the first item), and a log out would not clear the logged-in screens'
 * state.
 */
@Composable
internal fun MainNavHost(navigationState: NavigationState, navigator: Navigator, modifier: Modifier = Modifier) {
    NavDisplay(
        modifier = modifier,
        onBack = { navigator.goBackOrToStart() },
        entries = navigationState.toEntries(
            entryProvider {
                entry<CollectionListRoute> {
                    CollectionListScreen(
                        onCollectionClick = { id -> navigator.navigate(CollectionRoute(id)) },
                        onNewCollectionClick = { navigator.navigate(NewCollectionRoute()) },
                        onSearchClick = { navigator.navigate(ItemSearchRoute) }
                    )
                }
                entry<AlbumListRoute> {
                    AlbumListScreen(
                        onAlbumClick = { id -> navigator.navigate(AlbumRoute(id)) },
                        onNewAlbumClick = { navigator.navigate(NewAlbumRoute()) },
                        onBack = { navigator.goBackOrToStart() }
                    )
                }
                entry<AlbumRoute> { route ->
                    AlbumScreen(
                        albumId = route.albumId,
                        onAlbumClick = { id -> navigator.navigate(AlbumRoute(id)) },
                        onPhotoClick = { id -> navigator.navigate(PhotoRoute(id)) },
                        onNewAlbumClick = { navigator.navigate(NewAlbumRoute(route.albumId)) },
                        onNewPhotoClick = { navigator.navigate(NewPhotoRoute(route.albumId)) },
                        onEditClick = { navigator.navigate(EditAlbumRoute(route.albumId)) },
                        onBack = { navigator.goBackOrToStart() },
                        // The album is gone; the list or the parent reads again (AlbumChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<PhotoRoute> { route ->
                    PhotoScreen(
                        photoId = route.photoId,
                        onEditClick = { navigator.navigate(EditPhotoRoute(route.photoId)) },
                        onBack = { navigator.goBackOrToStart() },
                        // The photo is gone; its album reads again (PhotoChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<NewPhotoRoute> { route ->
                    NewPhotoScreen(
                        albumId = route.albumId,
                        onBack = { navigator.goBackOrToStart() },
                        // The album screen reads again (PhotoChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<EditPhotoRoute> { route ->
                    EditPhotoScreen(
                        photoId = route.photoId,
                        onBack = { navigator.goBackOrToStart() },
                        // The photo screen and its album screen read again (PhotoChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<NewAlbumRoute> { route ->
                    NewAlbumScreen(
                        parentId = route.parentId,
                        onBack = { navigator.goBackOrToStart() },
                        // The list or the parent reads its list again (AlbumChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<EditAlbumRoute> { route ->
                    EditAlbumScreen(
                        albumId = route.albumId,
                        onBack = { navigator.goBackOrToStart() },
                        // The album screen and the list or the parent read again (AlbumChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<WishlistListRoute> {
                    WishlistListScreen(
                        onWishlistClick = { id -> navigator.navigate(WishlistRoute(id)) },
                        onNewWishlistClick = { navigator.navigate(NewWishlistRoute()) },
                        onBack = { navigator.goBackOrToStart() }
                    )
                }
                entry<WishlistRoute> { route ->
                    WishlistScreen(
                        wishlistId = route.wishlistId,
                        onWishlistClick = { id -> navigator.navigate(WishlistRoute(id)) },
                        onWishClick = { id -> navigator.navigate(WishRoute(id)) },
                        onNewWishlistClick = { navigator.navigate(NewWishlistRoute(route.wishlistId)) },
                        onNewWishClick = { navigator.navigate(NewWishRoute(route.wishlistId)) },
                        onEditClick = { navigator.navigate(EditWishlistRoute(route.wishlistId)) },
                        onBack = { navigator.goBackOrToStart() },
                        // The wishlist is gone; the list or the parent reads again (WishlistChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<WishRoute> { route ->
                    WishScreen(
                        wishId = route.wishId,
                        onEditClick = { navigator.navigate(EditWishRoute(route.wishId)) },
                        onBack = { navigator.goBackOrToStart() },
                        // The wish is gone; its wishlist reads again (WishChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<NewWishRoute> { route ->
                    NewWishScreen(
                        wishlistId = route.wishlistId,
                        onBack = { navigator.goBackOrToStart() },
                        // The wishlist screen reads again (WishChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<EditWishRoute> { route ->
                    EditWishScreen(
                        wishId = route.wishId,
                        onBack = { navigator.goBackOrToStart() },
                        // The wish screen and its wishlist screen read again (WishChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<NewWishlistRoute> { route ->
                    NewWishlistScreen(
                        parentId = route.parentId,
                        onBack = { navigator.goBackOrToStart() },
                        // The list or the parent reads its list again (WishlistChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<EditWishlistRoute> { route ->
                    EditWishlistScreen(
                        wishlistId = route.wishlistId,
                        onBack = { navigator.goBackOrToStart() },
                        // The wishlist screen and the list or the parent read again (WishlistChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<ItemSearchRoute> {
                    ItemSearchScreen(
                        onItemClick = { id -> navigator.navigate(ItemRoute(id)) },
                        onBack = { navigator.goBackOrToStart() }
                    )
                }
                entry<TagListRoute> {
                    TagListScreen(
                        onTagClick = { tag -> navigator.navigate(TagItemsRoute(tagId = tag.id, label = tag.label)) }
                    )
                }
                entry<TagItemsRoute> { route ->
                    TagItemsScreen(
                        tagId = route.tagId,
                        label = route.label,
                        onItemClick = { id -> navigator.navigate(ItemRoute(id)) }
                    )
                }
                entry<TemplateListRoute> {
                    TemplateListScreen(
                        onTemplateClick = { template ->
                            navigator.navigate(TemplateRoute(templateId = template.id, name = template.name))
                        },
                        onNewTemplateClick = { navigator.navigate(TemplateFormRoute()) },
                        onChoiceListsClick = { navigator.navigate(ChoiceListListRoute) }
                    )
                }
                entry<TemplateRoute> { route ->
                    TemplateScreen(
                        templateId = route.templateId,
                        name = route.name,
                        onEdit = { navigator.navigate(TemplateFormRoute(route.templateId)) },
                        // The template is gone; the list reads its list again (TemplateChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<TemplateFormRoute> { route ->
                    TemplateFormScreen(
                        templateId = route.templateId,
                        onBack = { navigator.goBackOrToStart() },
                        // An edit goes back to the template screen, which reads it again (TemplateChanges).
                        // A new template replaces the form, so back goes to the list.
                        onSave = { id ->
                            navigator.close(route)
                            if (route.templateId == null) navigator.navigate(TemplateRoute(templateId = id, name = ""))
                        },
                        onChoiceListsClick = { navigator.navigate(ChoiceListListRoute) }
                    )
                }
                entry<ChoiceListListRoute> {
                    ChoiceListListScreen(
                        onChoiceListClick = { id -> navigator.navigate(ChoiceListFormRoute(id)) },
                        onNewChoiceListClick = { navigator.navigate(ChoiceListFormRoute()) }
                    )
                }
                entry<ChoiceListFormRoute> { route ->
                    ChoiceListFormScreen(
                        choiceListId = route.choiceListId,
                        // The screens below read the choice lists again (ChoiceListChanges).
                        onDone = { navigator.close(route) }
                    )
                }
                entry<CollectionRoute> { route ->
                    CollectionScreen(
                        collectionId = route.collectionId,
                        onCollectionClick = { id -> navigator.navigate(CollectionRoute(id)) },
                        onItemClick = { id -> navigator.navigate(ItemRoute(id)) },
                        onAddItemClick = { navigator.navigate(AddItemRoute(route.collectionId)) },
                        onNewCollectionClick = { navigator.navigate(NewCollectionRoute(route.collectionId)) },
                        onEditClick = { navigator.navigate(EditCollectionRoute(route.collectionId)) },
                        onBack = { navigator.goBackOrToStart() },
                        // The collection is gone; the list or the parent reads it again (CollectionListChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<ItemRoute> { route ->
                    ItemScreen(
                        itemId = route.itemId,
                        onBack = { navigator.goBackOrToStart() },
                        // The item is gone; its collection reads its list again (ItemListChanges).
                        onClose = { navigator.close(route) },
                        onEdit = { navigator.navigate(EditItemRoute(route.itemId)) },
                        loans = {
                            LoansSection(
                                itemId = route.itemId,
                                onNewLoan = { navigator.navigate(LoanFormRoute(route.itemId)) },
                                onLoanClick = { id -> navigator.navigate(LoanFormRoute(route.itemId, id)) }
                            )
                        }
                    )
                }
                entry<LoanFormRoute> { route ->
                    LoanFormScreen(
                        route = route,
                        onBack = { navigator.goBackOrToStart() },
                        // The loans section reads again (LoanChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<EditItemRoute> { route ->
                    EditItemScreen(itemId = route.itemId, onBack = { navigator.goBackOrToStart() })
                }
                entry<NewCollectionRoute> { route ->
                    NewCollectionScreen(
                        parentId = route.parentId,
                        onBack = { navigator.goBackOrToStart() },
                        // The new collection replaces the form, so back goes to the list or the parent.
                        onOpenCollection = { id ->
                            navigator.close(route)
                            navigator.navigate(CollectionRoute(id))
                        }
                    )
                }
                entry<EditCollectionRoute> { route ->
                    EditCollectionScreen(
                        collectionId = route.collectionId,
                        onBack = { navigator.goBackOrToStart() },
                        // The collection screen and the list read the collection again (CollectionChanges).
                        onClose = { navigator.close(route) }
                    )
                }
                entry<SettingsRoute> {
                    // A section root: the top bar shows the menu, and back goes to Collections.
                    SettingsScreen(navigationIcon = NavigationIconConfig.Menu)
                }
                entry<AddItemRoute> { route ->
                    AddItemScreen(
                        collectionId = route.collectionId,
                        onBack = { navigator.goBackOrToStart() },
                        // The new item replaces the form, so back goes to the collection.
                        onOpenItem = { id -> navigator.replaceCurrent(ItemRoute(id)) }
                    )
                }
            }
        )
    )
}
