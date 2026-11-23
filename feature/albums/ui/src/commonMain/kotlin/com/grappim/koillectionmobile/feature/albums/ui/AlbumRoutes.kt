package com.grappim.koillectionmobile.feature.albums.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The routes this module owns. Each one is registered in the shell's `navKeySerializersModule`:
 * a route missing from there only breaks back-stack restore after process death.
 */
@Serializable
data object AlbumListRoute : NavKey

/** The id is all it carries. The screen reads the album again, not a snapshot of the list. */
@Serializable
data class AlbumRoute(val albumId: String) : NavKey
