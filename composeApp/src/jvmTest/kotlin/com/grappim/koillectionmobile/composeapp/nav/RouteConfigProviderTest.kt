package com.grappim.koillectionmobile.composeapp.nav

import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.settings.ui.SettingsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
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
    fun `only the collection list and Settings use the shell top bar so far`() {
        assertTrue(RouteConfigProvider.getConfig(CollectionListRoute).hasShellTopBar)
        assertTrue(RouteConfigProvider.getConfig(SettingsRoute).hasShellTopBar)
        assertFalse(RouteConfigProvider.getConfig(TagListRoute).hasShellTopBar)
        assertFalse(RouteConfigProvider.getConfig(CollectionRoute("c")).hasShellTopBar)
    }
}
