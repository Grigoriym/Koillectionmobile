package com.grappim.koillectionmobile.composeapp.di

import com.grappim.koillectionmobile.core.api.NetworkModule
import com.grappim.koillectionmobile.core.storage.PlatformStorageModule
import com.grappim.koillectionmobile.feature.auth.data.AuthDataModule
import com.grappim.koillectionmobile.feature.auth.ui.AuthUiModule
import com.grappim.koillectionmobile.feature.collections.data.CollectionsDataModule
import com.grappim.koillectionmobile.feature.collections.mapper.CollectionsMapperModule
import com.grappim.koillectionmobile.feature.items.data.ItemsDataModule
import com.grappim.koillectionmobile.feature.items.mapper.ItemsMapperModule
import com.grappim.koillectionmobile.feature.tags.data.TagsDataModule
import com.grappim.koillectionmobile.feature.tags.mapper.TagsMapperModule
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
        ItemsMapperModule::class,
        ItemsDataModule::class,
        TagsMapperModule::class,
        TagsDataModule::class
    ]
)
@Configuration
@ComponentScan("com.grappim.koillectionmobile.composeapp")
class AppModule

@KoinApplication
object KoinApp
