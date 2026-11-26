package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_new
import com.grappim.koillectionmobile.strings.generated.resources.collections_empty
import com.grappim.koillectionmobile.strings.generated.resources.collections_title
import com.grappim.koillectionmobile.strings.generated.resources.item_search_title
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The start section of the drawer. The top bar is the shell's: this screen sets its title and the
 * search action.
 */
@Composable
fun CollectionListScreen(
    onCollectionClick: (String) -> Unit,
    onNewCollectionClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarController = LocalTopBarConfig.current
    val searchLabel = stringResource(RString.item_search_title)
    val currentOnSearchClick by rememberUpdatedState(onSearchClick)

    LaunchedEffect(searchLabel) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.collections_title),
                navigationIcon = NavigationIconConfig.Menu,
                actions = persistentListOf(
                    TopBarActionVectorButton(
                        imageVector = Icons.Filled.Search,
                        contentDescription = searchLabel,
                        onClick = { currentOnSearchClick() }
                    )
                )
            )
        )
    }

    CollectionListContent(
        uiState = uiState,
        onCollectionClick = onCollectionClick,
        onNewCollectionClick = onNewCollectionClick,
        modifier = modifier
    )
}

@Composable
private fun CollectionListContent(
    uiState: CollectionListUiState,
    onCollectionClick: (String) -> Unit,
    onNewCollectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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

internal val PADDING = 16.dp
