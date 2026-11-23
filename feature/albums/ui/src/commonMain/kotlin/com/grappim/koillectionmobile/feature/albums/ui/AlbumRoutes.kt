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

/** The new-album form. [parentId] is the album the new one goes into, or null for the top level. */
@Serializable
data class NewAlbumRoute(val parentId: String? = null) : NavKey

/** The edit form of album [albumId]. */
@Serializable
data class EditAlbumRoute(val albumId: String) : NavKey

/** The screen of photo [photoId]. */
@Serializable
data class PhotoRoute(val photoId: String) : NavKey
