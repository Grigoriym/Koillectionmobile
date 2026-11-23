package com.grappim.koillectionmobile.feature.albums.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_empty
import com.grappim.koillectionmobile.strings.generated.resources.album_section_children
import com.grappim.koillectionmobile.strings.generated.resources.album_section_photos
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AlbumScreen(
    albumId: String,
    onAlbumClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = koinViewModel { parametersOf(albumId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AlbumContent(uiState = uiState, onAlbumClick = onAlbumClick, onBack = onBack, modifier = modifier)
}

/** The child albums as full-width rows, then the photos as a grid, in one scrolling list. */
@Composable
private fun AlbumContent(
    uiState: AlbumUiState,
    onAlbumClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopBar(title = uiState.title, onBack = onBack) }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || (uiState.children.isEmpty() && uiState.photos.isEmpty()) -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.album_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyVerticalGrid(
                modifier = contentModifier,
                columns = GridCells.Adaptive(PHOTO_MIN_SIZE),
                contentPadding = PaddingValues(bottom = PADDING),
                horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
                verticalArrangement = Arrangement.spacedBy(GRID_SPACING)
            ) {
                if (uiState.children.isNotEmpty()) {
                    fullWidth("children-header") { SectionHeader(stringResource(RString.album_section_children)) }
                    uiState.children.forEach { child ->
                        fullWidth("album-${child.id}") {
                            ListItem(
                                modifier = Modifier.clickable { onAlbumClick(child.id) },
                                headlineContent = { Text(child.title) },
                                leadingContent = { Thumbnail(imageUrl = child.imageUrl, title = child.title) }
                            )
                        }
                    }
                }
                if (uiState.photos.isNotEmpty()) {
                    fullWidth("photos-header") { SectionHeader(stringResource(RString.album_section_photos)) }
                    items(uiState.photos, key = { "photo-${it.id}" }) { photo ->
                        PhotoCell(photo = photo, modifier = Modifier.padding(horizontal = GRID_SPACING))
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** A square thumbnail (`imageSmallThumbnail`) with the title under it. */
@Composable
private fun PhotoCell(photo: Photo, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Thumbnail(
            imageUrl = photo.thumbnailUrl,
            title = photo.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Text(
            text = photo.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(PADDING),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

private val PHOTO_MIN_SIZE = 112.dp
private val GRID_SPACING = 4.dp
