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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionMenu
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarMenuItem
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.strings.RPlurals
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_delete
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_children
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_photos
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.album_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.album_edit
import com.grappim.koillectionmobile.strings.generated.resources.album_empty
import com.grappim.koillectionmobile.strings.generated.resources.album_more
import com.grappim.koillectionmobile.strings.generated.resources.album_new
import com.grappim.koillectionmobile.strings.generated.resources.album_section_children
import com.grappim.koillectionmobile.strings.generated.resources.album_section_photos
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.photo_new
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onClose] runs once the server deleted the album, to take the screen off the back stack. */
@Composable
fun AlbumScreen(
    albumId: String,
    onAlbumClick: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    onNewAlbumClick: () -> Unit,
    onNewPhotoClick: () -> Unit,
    onEditClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = koinViewModel { parametersOf(albumId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    AlbumContent(
        uiState = uiState,
        onAlbumClick = onAlbumClick,
        onPhotoClick = onPhotoClick,
        onNewAlbumClick = onNewAlbumClick,
        onNewPhotoClick = onNewPhotoClick,
        onEditClick = onEditClick,
        modifier = modifier
    )
}

/** The child albums as full-width rows, then the photos as a grid, in one scrolling list. */
@Composable
private fun AlbumContent(
    uiState: AlbumUiState,
    onAlbumClick: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    onNewAlbumClick: () -> Unit,
    onNewPhotoClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarController = LocalTopBarConfig.current
    val deleteLabel = stringResource(RString.album_delete)
    val moreLabel = stringResource(RString.album_more)
    val currentOnDeleteClick by rememberUpdatedState(uiState.onDeleteClick)
    val currentOnNewAlbumClick by rememberUpdatedState(onNewAlbumClick)
    val currentOnNewPhotoClick by rememberUpdatedState(onNewPhotoClick)
    val currentOnEditClick by rememberUpdatedState(onEditClick)
    // The kit bar has no progress slot: while the delete runs, Delete stays, disabled, and the
    // progress shows under the bar.
    val isBusy = uiState.isCountingDelete || uiState.isDeleting
    val hasDelete = uiState.canDelete || isBusy
    LaunchedEffect(uiState.title, hasDelete, isBusy, deleteLabel, moreLabel) {
        val menu = TopBarActionMenu(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = moreLabel,
            items = persistentListOf(
                TopBarMenuItem(
                    text = NativeText.Resource(RString.album_new),
                    onClick = { currentOnNewAlbumClick() }
                ),
                TopBarMenuItem(
                    text = NativeText.Resource(RString.photo_new),
                    onClick = { currentOnNewPhotoClick() }
                ),
                TopBarMenuItem(
                    text = NativeText.Resource(RString.album_edit),
                    onClick = { currentOnEditClick() }
                )
            )
        )
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(uiState.title),
                navigationIcon = NavigationIconConfig.Back(),
                actions = if (hasDelete) {
                    persistentListOf(
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = deleteLabel,
                            enabled = !isBusy,
                            onClick = { currentOnDeleteClick() }
                        ),
                        menu
                    )
                } else {
                    persistentListOf(menu)
                }
            )
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
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
                        PhotoCell(
                            photo = photo,
                            modifier = Modifier
                                .clickable { onPhotoClick(photo.id) }
                                .padding(horizontal = GRID_SPACING)
                        )
                    }
                }
            }
        }
    }
    DeleteDialogs(uiState)
}

@Composable
private fun DeleteDialogs(uiState: AlbumUiState) {
    uiState.deleteSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = uiState.onDeleteDismiss,
            title = { Text(stringResource(RString.album_delete_title)) },
            text = {
                val children =
                    pluralStringResource(RPlurals.album_delete_children, summary.childCount, summary.childCount)
                val photos =
                    pluralStringResource(RPlurals.album_delete_photos, summary.photoCount, summary.photoCount)
                Text(stringResource(RString.album_delete_text, uiState.title, children, photos))
            },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteConfirm) {
                    Text(stringResource(RString.collection_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = uiState.onDeleteDismiss) {
                    Text(stringResource(RString.collection_delete_cancel))
                }
            }
        )
    }
    if (uiState.deleteError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteErrorShown,
            title = { Text(stringResource(RString.album_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.collection_delete_failed_ok))
                }
            }
        )
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
