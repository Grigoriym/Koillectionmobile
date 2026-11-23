package com.grappim.koillectionmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import com.grappim.koillectionmobile.feature.albums.ui.AlbumListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateListRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistListRoute

/**
 * One entry per drawer item. A section has a [route] and its own back stack. [LogOut] has no
 * route: it is an action, not a section (plan §5 decision 15).
 */
enum class DrawerDestination(val route: NavKey?) {
    Collections(CollectionListRoute),
    Tags(TagListRoute),
    Templates(TemplateListRoute),
    Wishlists(WishlistListRoute),
    Albums(AlbumListRoute),
    Settings(SettingsRoute),
    LogOut(null)
}

/**
 * The set `rememberNavigationState` builds a back stack for. `NavigationState.currentSubStack`
 * fails on a section that has no back stack, so this is built from [DrawerDestination].
 */
val DRAWER_NAV_ITEMS: Set<NavKey> = DrawerDestination.entries.mapNotNull { it.route }.toSet()
