package com.grappim.koillectionmobile.feature.albums.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_new
import com.grappim.koillectionmobile.strings.generated.resources.albums_empty
import com.grappim.koillectionmobile.strings.generated.resources.albums_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumListScreen(
    onAlbumClick: (String) -> Unit,
    onNewAlbumClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AlbumListContent(
        uiState = uiState,
        onAlbumClick = onAlbumClick,
        onNewAlbumClick = onNewAlbumClick,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun AlbumListContent(
    uiState: AlbumListUiState,
    onAlbumClick: (String) -> Unit,
    onNewAlbumClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopBar(title = stringResource(RString.albums_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewAlbumClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(RString.album_new))
            }
        }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.albums.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.albums_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.albums, key = { it.id }) { album ->
                    ListItem(
                        modifier = Modifier.clickable { onAlbumClick(album.id) },
                        headlineContent = { Text(album.title) },
                        leadingContent = { Thumbnail(imageUrl = album.imageUrl, title = album.title) }
                    )
                }
            }
        }
    }
}
