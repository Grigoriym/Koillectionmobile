package com.grappim.koillectionmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.items.ui.AddItemRoute
import com.grappim.koillectionmobile.feature.items.ui.ItemRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
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
        subclass(ItemRoute::class)
        subclass(AddItemRoute::class)
        subclass(SettingsRoute::class)
    }
}

internal val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = navKeySerializersModule
}
