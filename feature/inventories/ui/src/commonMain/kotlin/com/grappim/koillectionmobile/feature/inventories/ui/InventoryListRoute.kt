package com.grappim.koillectionmobile.feature.inventories.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The inventory list, the root of the Inventories drawer section. It is registered in the shell's
 * `navKeySerializersModule`: a route missing from there only breaks back-stack restore after
 * process death.
 */
@Serializable
data object InventoryListRoute : NavKey
