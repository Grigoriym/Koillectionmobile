package com.grappim.koillectionmobile.feature.tags.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The routes this module owns. Each one is registered in the shell's `navKeySerializersModule`:
 * a route missing from there only breaks back-stack restore after process death.
 */
@Serializable
data object TagListRoute : NavKey

/** The items of tag [tagId]. [label] is the screen title, as the tag list showed it. */
@Serializable
data class TagItemsRoute(val tagId: String, val label: String) : NavKey
