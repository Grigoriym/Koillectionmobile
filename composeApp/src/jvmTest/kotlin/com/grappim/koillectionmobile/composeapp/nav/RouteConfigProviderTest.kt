package com.grappim.koillectionmobile.composeapp.nav

import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagItemsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListFormRoute
import com.grappim.koillectionmobile.feature.templates.ui.ChoiceListListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateFormRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateListRoute
import com.grappim.koillectionmobile.feature.templates.ui.TemplateRoute
import com.grappim.koillectionmobile.feature.wishlists.ui.WishlistListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteConfigProviderTest {

    @Test
    fun `a swipe opens the drawer on every section root`() {
        DRAWER_NAV_ITEMS.forEach { route ->
            assertEquals(DrawerConfig.Enabled, RouteConfigProvider.getConfig(route).drawerConfig, "$route")
        }
    }

    @Test
    fun `a swipe does not open the drawer below a section root`() {
        assertEquals(DrawerConfig.GesturesDisabled, RouteConfigProvider.getConfig(CollectionRoute("c")).drawerConfig)
    }

    @Test
    fun `the collection list and Settings and every Tags and Templates screen use the shell top bar`() {
        listOf(
            CollectionListRoute,
            SettingsRoute,
            TagListRoute,
            TagItemsRoute(tagId = "t", label = "l"),
            TemplateListRoute,
            TemplateRoute(templateId = "t", name = "n"),
            TemplateFormRoute(),
            ChoiceListListRoute,
            ChoiceListFormRoute()
        ).forEach { route ->
            assertTrue(RouteConfigProvider.getConfig(route).hasShellTopBar, "$route")
        }
    }

    @Test
    fun `a screen that still draws its own top bar hides the shell top bar`() {
        assertFalse(RouteConfigProvider.getConfig(WishlistListRoute).hasShellTopBar)
        assertFalse(RouteConfigProvider.getConfig(CollectionRoute("c")).hasShellTopBar)
    }
}
