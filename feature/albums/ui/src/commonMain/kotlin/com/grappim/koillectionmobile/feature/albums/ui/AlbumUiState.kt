package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.Photo

/**
 * One album: its [title], its direct [children], then its [photos], each sorted by title ignoring
 * case. A load fills all three or none: a failed load or retry clears them and sets [error].
 */
data class AlbumUiState(
    val title: String = "",
    val children: List<Album> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
