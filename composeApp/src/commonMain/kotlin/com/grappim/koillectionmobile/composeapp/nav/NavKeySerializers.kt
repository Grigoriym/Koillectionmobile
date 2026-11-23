package com.grappim.koillectionmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.grappim.koillectionmobile.feature.albums.ui.AlbumListRoute
import com.grappim.koillectionmobile.feature.albums.ui.AlbumRoute
import com.grappim.koillectionmobile.feature.albums.ui.EditAlbumRoute
import com.grappim.koillectionmobile.feature.albums.ui.EditPhotoRoute
import com.grappim.koillectionmobile.feature.albums.ui.NewAlbumRoute
import com.grappim.koillectionmobile.feature.albums.ui.NewPhotoRoute
import com.grappim.koillectionmobile.feature.albums.ui.PhotoRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.collections.ui.EditCollectionRoute
import com.grappim.koillectionmobile.feature.collections.ui.ItemSearchRoute
import com.grappim.koillectionmobile.feature.collections.ui.NewCollectionRoute
import com.grappim.koillectionmobile.feature.items.ui.AddItemRoute
import com.grappim.koillectionmobile.feature.items.ui.EditItemRoute
import com.grappim.koillectionmobile.feature.items.ui.ItemRoute
import com.grappim.koillectionmobile.feature.loans.ui.LoanFormRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagItemsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListFormRoute
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateFormRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.EditWishRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.EditWishlistRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.NewWishRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.NewWishlistRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistListRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Every route in the app, listed once. `rememberNavBackStack` throws on the first composition if
 * it gets `SavedStateConfiguration.DEFAULT`. A route missing from here is the quiet failure: the
 * app runs, and only back-stack restore after process death breaks.
 */
private val navKeySerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CollectionListRoute::class)
        subclass(CollectionRoute::class)
        subclass(NewCollectionRoute::class)
        subclass(EditCollectionRoute::class)
        subclass(ItemSearchRoute::class)
        subclass(ItemRoute::class)
        subclass(AddItemRoute::class)
        subclass(EditItemRoute::class)
        subclass(LoanFormRoute::class)
        subclass(SettingsRoute::class)
        subclass(TagListRoute::class)
        subclass(TagItemsRoute::class)
        subclass(TemplateFormRoute::class)
        subclass(ChoiceListListRoute::class)
        subclass(ChoiceListFormRoute::class)
        subclass(TemplateListRoute::class)
        subclass(TemplateRoute::class)
        subclass(WishlistListRoute::class)
        subclass(WishlistRoute::class)
        subclass(NewWishlistRoute::class)
        subclass(EditWishlistRoute::class)
        subclass(WishRoute::class)
        subclass(NewWishRoute::class)
        subclass(EditWishRoute::class)
        subclass(AlbumListRoute::class)
        subclass(AlbumRoute::class)
        subclass(NewAlbumRoute::class)
        subclass(EditAlbumRoute::class)
        subclass(PhotoRoute::class)
        subclass(NewPhotoRoute::class)
        subclass(EditPhotoRoute::class)
    }
}

internal val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}
