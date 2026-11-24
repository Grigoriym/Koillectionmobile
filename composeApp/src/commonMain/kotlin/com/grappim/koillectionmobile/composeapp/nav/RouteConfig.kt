package com.grappim.koillectionmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import com.grappim.koillectionmobile.feature.albums.ui.AlbumListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateListRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistListRoute

/** How the drawer behaves on a given route. */
sealed interface DrawerConfig {
    /** A swipe opens the drawer as well as the menu button: the section roots. */
    data object Enabled : DrawerConfig

    /** A horizontal swipe belongs to the screen: every screen below a section root. */
    data object GesturesDisabled : DrawerConfig
}

data class RouteConfig(val drawerConfig: DrawerConfig = DrawerConfig.GesturesDisabled)

/** The shell's settings per route. A route that is not named here gets the defaults. */
object RouteConfigProvider {

    fun getConfig(route: NavKey): RouteConfig = when (route) {
        CollectionListRoute, SettingsRoute, TagListRoute, TemplateListRoute, WishlistListRoute, AlbumListRoute ->
            RouteConfig(drawerConfig = DrawerConfig.Enabled)

        else -> RouteConfig()
    }
}
