package com.grappim.koillectionmobile.composeapp.nav

import androidx.navigation3.runtime.NavKey
import com.grappim.kit.navigation.Navigator

/**
 * Back at the root of a section other than the start section goes to the start section. The kit's
 * `goBack` returns `false` there, and the system back then closes the app.
 *
 * @return false only at the root of the start section.
 */
internal fun Navigator.goBackOrToStart(): Boolean {
    if (goBack()) return true
    if (state.currentTopLevelKey::class == state.startKey::class) return false
    navigate(state.startKey)
    return true
}

/**
 * Removes [route] from every section's back stack, wherever it is. A screen closes itself with this
 * when its work is done (a delete, a save). The section can change while the work runs, so the
 * route is not always on top of the current back stack.
 */
internal fun Navigator.close(route: NavKey) {
    state.subStacks.values.forEach { stack ->
        if (stack.size > 1 && stack.first() != route) stack.remove(route)
    }
}
