package com.grappim.koillectionmobile.feature.settings.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Registered in the shell's `navKeySerializersModule`, like every route. The login screen opens
 * the same screen as state, not through this route: login has no back stack.
 */
@Serializable
data object SettingsRoute : NavKey
