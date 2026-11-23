package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.albums.domain.Album

/**
 * The top-level albums, sorted by title ignoring case. A failed load or retry clears [albums] and
 * sets [error].
 */
data class AlbumListUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
