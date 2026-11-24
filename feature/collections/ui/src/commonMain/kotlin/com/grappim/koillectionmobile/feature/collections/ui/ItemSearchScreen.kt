package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_search_no_match
import com.grappim.koillectionmobile.strings.generated.resources.item_search_empty
import com.grappim.koillectionmobile.strings.generated.resources.item_search_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ItemSearchScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemSearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ItemSearchContent(uiState = uiState, onItemClick = onItemClick, modifier = modifier)
}

@Composable
private fun ItemSearchContent(
    uiState: ItemSearchUiState,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarController = LocalTopBarConfig.current
    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.item_search_title),
                navigationIcon = NavigationIconConfig.Back()
            )
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.results.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.item_search_empty),
                onRetry = uiState.onRetry,
                retryEnabled = true
            )

            else -> Column(modifier = contentModifier) {
                SearchField(query = uiState.query, onQueryChange = uiState.onQueryChange)
                ResultList(uiState = uiState, onItemClick = onItemClick, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultList(uiState: ItemSearchUiState, onItemClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val visibleResults = uiState.visibleResults
    LazyColumn(modifier = modifier) {
        if (visibleResults.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(PADDING),
                    text = stringResource(RString.collection_search_no_match, uiState.query.trim())
                )
            }
        }
        items(visibleResults, key = { it.itemId }) { result ->
            ListItem(
                modifier = Modifier.clickable { onItemClick(result.itemId) },
                headlineContent = { Text(result.name) },
                supportingContent = result.collectionTitle?.let { title -> { Text(title) } }
            )
        }
    }
}
