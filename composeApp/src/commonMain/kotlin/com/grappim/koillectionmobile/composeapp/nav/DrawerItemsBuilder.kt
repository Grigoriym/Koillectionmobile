package com.grappim.koillectionmobile.composeapp.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.drawer.DrawerItem
import com.grappim.kit.uikit.widgets.drawer.IconSource
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.albums_title
import com.grappim.koillectionmobile.strings.generated.resources.collections_title
import com.grappim.koillectionmobile.strings.generated.resources.drawer_log_out
import com.grappim.koillectionmobile.strings.generated.resources.inventories_title
import com.grappim.koillectionmobile.strings.generated.resources.settings_title
import com.grappim.koillectionmobile.strings.generated.resources.tags_title
import com.grappim.koillectionmobile.strings.generated.resources.templates_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.Factory

/**
 * A class and not a constant, so the item list can later depend on state (wallosmobile's
 * `DrawerItemsBuilder`). `material-icons-core` has no tag, template, album or wish icon, so some
 * icons are only near the meaning.
 */
@Factory
class DrawerItemsBuilder {

    fun build(): ImmutableList<DrawerItem<DrawerDestination>> = persistentListOf(
        item(DrawerDestination.Collections, RString.collections_title, IconSource.Vector(Icons.Filled.Home)),
        item(DrawerDestination.Tags, RString.tags_title, IconSource.Vector(Icons.Filled.Star)),
        item(DrawerDestination.Templates, RString.templates_title, IconSource.Vector(Icons.Filled.Create)),
        item(DrawerDestination.Wishlists, RString.wishlists_title, IconSource.Vector(Icons.Filled.Favorite)),
        item(DrawerDestination.Albums, RString.albums_title, IconSource.Vector(Icons.Filled.AccountBox)),
        item(
            DrawerDestination.Inventories,
            RString.inventories_title,
            IconSource.Vector(Icons.AutoMirrored.Filled.List)
        ),
        item(DrawerDestination.Settings, RString.settings_title, IconSource.Vector(Icons.Filled.Settings)),
        DrawerItem.Divider,
        item(
            DrawerDestination.LogOut,
            RString.drawer_log_out,
            IconSource.Vector(Icons.AutoMirrored.Filled.ExitToApp)
        )
    )

    private fun item(
        destination: DrawerDestination,
        label: StringResource,
        icon: IconSource
    ): DrawerItem<DrawerDestination> = DrawerItem.Destination(
        destination = destination,
        label = NativeText.Resource(label),
        icon = icon
    )
}
