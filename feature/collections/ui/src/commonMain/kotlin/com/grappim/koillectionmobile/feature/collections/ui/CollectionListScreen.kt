package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_empty
import com.grappim.koillectionmobile.strings.generated.resources.collections_retry
import com.grappim.koillectionmobile.strings.generated.resources.collections_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CollectionListScreen(
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionListContent(uiState = uiState, onCollectionClick = onCollectionClick, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionListContent(
    uiState: CollectionListUiState,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(RString.collections_title)) }) }
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            isRefreshing = uiState.isRefreshing,
            onRefresh = uiState.onRefresh
        ) {
            when {
                uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                uiState.collections.isEmpty() -> EmptyOrError(uiState = uiState)

                else -> CollectionList(uiState = uiState, onCollectionClick = onCollectionClick)
            }
        }
    }
}

/** Scrolls, so the pull to refresh works here too. */
@Composable
private fun EmptyOrError(uiState: CollectionListUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically)
    ) {
        if (uiState.error.isNotEmpty()) {
            Text(
                text = uiState.error.asString(),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = uiState.onRefresh, enabled = !uiState.isRefreshing) {
                Text(stringResource(RString.collections_retry))
            }
        } else {
            Text(text = stringResource(RString.collections_empty), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CollectionList(
    uiState: CollectionListUiState,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (uiState.error.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(PADDING),
                    text = uiState.error.asString(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        items(uiState.collections, key = { it.id }) { collection ->
            ListItem(
                modifier = Modifier.clickable { onCollectionClick(collection.id) },
                headlineContent = { Text(collection.title) },
                leadingContent = { CollectionImage(collection = collection) }
            )
        }
    }
}

/** Without an image, the first letter of the title. */
@Composable
private fun CollectionImage(collection: Collection, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(IMAGE_SIZE)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (collection.imageUrl != null) {
            AsyncImage(
                model = collection.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = collection.title.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val PADDING = 16.dp
private val IMAGE_SIZE = 48.dp
