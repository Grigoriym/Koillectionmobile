package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_add_item
import com.grappim.koillectionmobile.strings.generated.resources.collection_back
import com.grappim.koillectionmobile.strings.generated.resources.collection_empty
import com.grappim.koillectionmobile.strings.generated.resources.collection_section_children
import com.grappim.koillectionmobile.strings.generated.resources.collection_section_items
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionScreen(
    collectionId: String,
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(collectionId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionContent(
        uiState = uiState,
        onCollectionClick = onCollectionClick,
        onItemClick = onItemClick,
        onAddItemClick = onAddItemClick,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionContent(
    uiState: CollectionUiState,
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(RString.collection_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(RString.collection_add_item))
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

            uiState.error.isNotEmpty() || (uiState.children.isEmpty() && uiState.items.isEmpty()) -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.collection_empty),
                onRetry = uiState.onRetry,
                retryEnabled = true
            )

            else -> LazyColumn(modifier = contentModifier) {
                if (uiState.children.isNotEmpty()) {
                    item { SectionHeader(stringResource(RString.collection_section_children)) }
                    items(uiState.children, key = { "collection-${it.id}" }) { child ->
                        ListItem(
                            modifier = Modifier.clickable { onCollectionClick(child.id) },
                            headlineContent = { Text(child.title) },
                            leadingContent = { Thumbnail(imageUrl = child.imageUrl, title = child.title) }
                        )
                    }
                }
                if (uiState.items.isNotEmpty()) {
                    item { SectionHeader(stringResource(RString.collection_section_items)) }
                    items(uiState.items, key = { "item-${it.id}" }) { item ->
                        ListItem(
                            modifier = Modifier.clickable { onItemClick(item.id) },
                            headlineContent = { Text(item.name) },
                            leadingContent = {
                                Thumbnail(
                                    imageUrl = item.image?.let { it.smallThumbnailUrl ?: it.url },
                                    title = item.name
                                )
                            }
                        )
                    }
                }
            }
        }
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
