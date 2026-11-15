package com.grappim.koillectionmobile.feature.items.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The add-item form for collection [collectionId]. Registered in the shell's
 * `navKeySerializersModule`: a route missing from there only breaks back-stack restore after
 * process death.
 */
@Serializable
data class AddItemRoute(val collectionId: String) : NavKey
