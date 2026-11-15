package com.grappim.koillectionmobile.feature.items.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Registered in the shell's `navKeySerializersModule`: a route missing from there only breaks
 * back-stack restore after process death.
 */
@Serializable
data class ItemRoute(val itemId: String) : NavKey
