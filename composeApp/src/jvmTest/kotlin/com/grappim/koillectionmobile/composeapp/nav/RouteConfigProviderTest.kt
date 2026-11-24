package com.grappim.koillectionmobile.composeapp.nav

import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
