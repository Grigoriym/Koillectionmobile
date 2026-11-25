package com.grappim.koillectionmobile.composeapp.di

import com.grappim.koillectionmobile.core.api.NetworkModule
import com.grappim.koillectionmobile.core.photos.PlatformPhotoModule
import com.grappim.koillectionmobile.core.storage.PlatformStorageModule
import com.grappim.koillectionmobile.feature.albums.data.AlbumsDataModule
import com.grappim.koillectionmobile.feature.albums.mapper.AlbumsMapperModule
import com.grappim.koillectionmobile.feature.albums.ui.AlbumsUiModule
import com.grappim.koillectionmobile.feature.auth.data.AuthDataModule
import com.grappim.koillectionmobile.feature.auth.ui.AuthUiModule
import com.grappim.koillectionmobile.feature.collections.data.CollectionsDataModule
import com.grappim.koillectionmobile.feature.collections.mapper.CollectionsMapperModule
import com.grappim.koillectionmobile.feature.collections.ui.CollectionsUiModule
import com.grappim.koillectionmobile.feature.inventories.data.InventoriesDataModule
import com.grappim.koillectionmobile.feature.inventories.mapper.InventoriesMapperModule
import com.grappim.koillectionmobile.feature.inventories.ui.InventoriesUiModule
import com.grappim.koillectionmobile.feature.items.data.ItemsDataModule
import com.grappim.koillectionmobile.feature.items.mapper.ItemsMapperModule
import com.grappim.koillectionmobile.feature.items.ui.ItemsUiModule
import com.grappim.koillectionmobile.feature.loans.data.LoansDataModule
import com.grappim.koillectionmobile.feature.loans.mapper.LoansMapperModule
import com.grappim.koillectionmobile.feature.loans.ui.LoansUiModule
import com.grappim.koillectionmobile.feature.settings.ui.SettingsUiModule
import com.grappim.koillectionmobile.feature.tags.data.TagsDataModule
import com.grappim.koillectionmobile.feature.tags.mapper.TagsMapperModule
import com.grappim.koillectionmobile.feature.tags.ui.TagsUiModule
import com.grappim.koillectionmobile.feature.templates.data.TemplatesDataModule
import com.grappim.koillectionmobile.feature.templates.mapper.TemplatesMapperModule
import com.grappim.koillectionmobile.feature.templates.ui.TemplatesUiModule
import com.grappim.koillectionmobile.feature.wishlists.data.WishlistsDataModule
import com.grappim.koillectionmobile.feature.wishlists.mapper.WishlistsMapperModule
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistsUiModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/**
 * The whole graph, listed once. Each feature's module class goes into `includes` here as it
 * appears. On JVM a `@Configuration` module in another Gradle module is found without this list
 * (M1.3: `KoinApp` still resolved `PlatformStorageModule` with it removed). Keep the list anyway:
 * other platforms are not checked.
 *
 * The `@ComponentScan` picks up this module's own definitions.
 */
@Module(
    includes = [
        NetworkModule::class,
        PlatformStorageModule::class,
        AuthDataModule::class,
        AuthUiModule::class,
        CollectionsMapperModule::class,
        CollectionsDataModule::class,
        CollectionsUiModule::class,
        ItemsMapperModule::class,
        ItemsDataModule::class,
        ItemsUiModule::class,
        PlatformPhotoModule::class,
        TagsMapperModule::class,
        TagsDataModule::class,
        TagsUiModule::class,
        TemplatesMapperModule::class,
        TemplatesDataModule::class,
        TemplatesUiModule::class,
        WishlistsMapperModule::class,
        WishlistsDataModule::class,
        WishlistsUiModule::class,
        LoansMapperModule::class,
        LoansDataModule::class,
        LoansUiModule::class,
        AlbumsMapperModule::class,
        AlbumsDataModule::class,
        AlbumsUiModule::class,
        InventoriesMapperModule::class,
        InventoriesDataModule::class,
        InventoriesUiModule::class,
        SettingsUiModule::class
    ]
)
@Configuration
@ComponentScan("com.grappim.koillectionmobile.composeapp")
class AppModule

@KoinApplication
object KoinApp
