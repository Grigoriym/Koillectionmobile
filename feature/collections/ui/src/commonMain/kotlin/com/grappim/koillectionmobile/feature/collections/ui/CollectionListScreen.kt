package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_more
import com.grappim.koillectionmobile.strings.generated.resources.collection_new
import com.grappim.koillectionmobile.strings.generated.resources.collections_empty
import com.grappim.koillectionmobile.strings.generated.resources.collections_log_out
import com.grappim.koillectionmobile.strings.generated.resources.collections_retry
import com.grappim.koillectionmobile.strings.generated.resources.collections_settings
import com.grappim.koillectionmobile.strings.generated.resources.collections_title
import com.grappim.koillectionmobile.strings.generated.resources.item_search_title
import com.grappim.koillectionmobile.strings.generated.resources.tags_title
import com.grappim.koillectionmobile.strings.generated.resources.templates_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CollectionListScreen(
    onCollectionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNewCollectionClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTagsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onWishlistsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionListContent(
        uiState = uiState,
        onCollectionClick = onCollectionClick,
        onSettingsClick = onSettingsClick,
        onNewCollectionClick = onNewCollectionClick,
        onSearchClick = onSearchClick,
        onTagsClick = onTagsClick,
        onTemplatesClick = onTemplatesClick,
        onWishlistsClick = onWishlistsClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionListContent(
    uiState: CollectionListUiState,
    onCollectionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNewCollectionClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTagsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onWishlistsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RString.collections_title)) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(RString.item_search_title)
                        )
                    }
                    MoreMenu(
                        listOf(
                            stringResource(RString.tags_title) to onTagsClick,
                            stringResource(RString.templates_title) to onTemplatesClick,
                            stringResource(RString.wishlists_title) to onWishlistsClick,
                            stringResource(RString.collections_settings) to onSettingsClick,
                            stringResource(RString.collections_log_out) to uiState.onLogOutClick
                        )
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCollectionClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(RString.collection_new))
            }
        }
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

                uiState.collections.isEmpty() -> EmptyOrError(
                    error = uiState.error,
                    emptyText = stringResource(RString.collections_empty),
                    onRetry = uiState.onRefresh,
                    retryEnabled = !uiState.isRefreshing
                )

                else -> CollectionList(uiState = uiState, onCollectionClick = onCollectionClick)
            }
        }
    }
}

/** Scrolls, so a pull to refresh works here too. */
@Composable
internal fun EmptyOrError(
    error: NativeText,
    emptyText: String,
    onRetry: () -> Unit,
    retryEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically)
    ) {
        if (error.isNotEmpty()) {
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry, enabled = retryEnabled) {
                Text(stringResource(RString.collections_retry))
            }
        } else {
            Text(text = emptyText, textAlign = TextAlign.Center)
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
                leadingContent = { Thumbnail(imageUrl = collection.imageUrl, title = collection.title) }
            )
        }
    }
}

/** The top bar's overflow menu: one entry per (label, action) pair, in order. */
@Composable
private fun MoreMenu(entries: List<Pair<String, () -> Unit>>) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(RString.collection_more))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            entries.forEach { (label, onClick) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        isExpanded = false
                        onClick()
                    }
                )
            }
        }
    }
}

internal val PADDING = 16.dp
