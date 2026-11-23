package com.grappim.koillectionmobile.composeapp.nav

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.grappim.kit.navigation.NavigationState
import com.grappim.kit.navigation.Navigator
import com.grappim.koillectionmobile.feature.collections.ui.CollectionListRoute
import com.grappim.koillectionmobile.feature.collections.ui.CollectionRoute
import com.grappim.koillectionmobile.feature.items.ui.ItemRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagItemsRoute
import com.grappim.koillectionmobile.feature.tags.ui.TagListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorExtTest {

    private fun navigator(): Navigator = Navigator(
        NavigationState(
            startKey = CollectionListRoute,
            topLevelStack = NavBackStack(CollectionListRoute),
            subStacks = DRAWER_NAV_ITEMS.associate { it::class to NavBackStack(it) }
        )
    )

    @Test
    fun `back at the start section root is left to the system`() {
        val navigator = navigator()

        assertFalse(navigator.goBackOrToStart())
        assertEquals(CollectionListRoute, navigator.state.currentKey)
    }

    @Test
    fun `back at another section root goes to the start section`() {
        val navigator = navigator()
        navigator.navigate(TagListRoute)

        assertTrue(navigator.goBackOrToStart())

        assertEquals(CollectionListRoute, navigator.state.currentTopLevelKey)
        assertEquals(CollectionListRoute, navigator.state.currentKey)
    }

    @Test
    fun `back below a section root goes up in that section`() {
        val navigator = navigator()
        navigator.navigate(TagListRoute)
        navigator.navigate(TagItemsRoute(tagId = "t", label = "Read"))

        assertTrue(navigator.goBackOrToStart())

        assertEquals(TagListRoute, navigator.state.currentKey)
    }

    @Test
    fun `each section keeps its own back stack`() {
        val navigator = navigator()
        navigator.navigate(CollectionRoute("books"))
        navigator.navigate(TagListRoute)
        navigator.navigate(TagItemsRoute(tagId = "t", label = "Read"))

        navigator.navigate(CollectionListRoute)

        assertEquals(CollectionRoute("books"), navigator.state.currentKey)
        navigator.navigate(TagListRoute)
        assertEquals(TagItemsRoute(tagId = "t", label = "Read"), navigator.state.currentKey)
    }

    @Test
    fun `close removes the route from another section and keeps the section roots`() {
        val navigator = navigator()
        navigator.navigate(ItemRoute("i"))
        navigator.navigate(TagListRoute)

        navigator.close(ItemRoute("i"))
        navigator.close(TagListRoute)

        assertEquals(listOf<NavKey>(CollectionListRoute), stackOf(navigator, CollectionListRoute))
        assertEquals(listOf<NavKey>(TagListRoute), stackOf(navigator, TagListRoute))
    }

    private fun stackOf(navigator: Navigator, root: NavKey): List<NavKey> =
        navigator.state.subStacks.getValue(root::class).toList()
}
