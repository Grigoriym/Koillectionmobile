package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RPlurals
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_add_item
import com.grappim.koillectionmobile.strings.generated.resources.collection_back
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_children
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_items
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_edit
import com.grappim.koillectionmobile.strings.generated.resources.collection_empty
import com.grappim.koillectionmobile.strings.generated.resources.collection_more
import com.grappim.koillectionmobile.strings.generated.resources.collection_new
import com.grappim.koillectionmobile.strings.generated.resources.collection_search_clear
import com.grappim.koillectionmobile.strings.generated.resources.collection_search_label
import com.grappim.koillectionmobile.strings.generated.resources.collection_search_no_match
import com.grappim.koillectionmobile.strings.generated.resources.collection_section_children
import com.grappim.koillectionmobile.strings.generated.resources.collection_section_items
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onClose] runs once the server deleted the collection, to take the screen off the back stack. */
@Composable
fun CollectionScreen(
    collectionId: String,
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onNewCollectionClick: () -> Unit,
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(collectionId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    CollectionContent(
        uiState = uiState,
        onCollectionClick = onCollectionClick,
        onItemClick = onItemClick,
        onAddItemClick = onAddItemClick,
        onNewCollectionClick = onNewCollectionClick,
        onEditClick = onEditClick,
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
    onNewCollectionClick: () -> Unit,
    onEditClick: () -> Unit,
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
                },
                actions = {
                    if (uiState.isCountingDelete || uiState.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.padding(PADDING / 2).size(PROGRESS_SIZE))
                    } else if (uiState.canDelete) {
                        IconButton(onClick = uiState.onDeleteClick) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(RString.collection_delete)
                            )
                        }
                    }
                    if (!uiState.isLoading && uiState.error.isEmpty() && !uiState.isDeleted) {
                        MoreMenu(onNewCollectionClick = onNewCollectionClick, onEditClick = onEditClick)
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

            else -> Column(modifier = contentModifier) {
                if (uiState.items.isNotEmpty()) {
                    SearchField(query = uiState.query, onQueryChange = uiState.onQueryChange)
                }
                CollectionList(
                    uiState = uiState,
                    onCollectionClick = onCollectionClick,
                    onItemClick = onItemClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    DeleteDialogs(uiState)
}

@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PADDING, end = PADDING, bottom = PADDING / 2),
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(RString.collection_search_label)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(RString.collection_search_clear)
                    )
                }
            }
        }
    )
}

@Composable
private fun CollectionList(
    uiState: CollectionUiState,
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = uiState.visibleItems
    LazyColumn(modifier = modifier) {
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
        if (visibleItems.isNotEmpty()) {
            item { SectionHeader(stringResource(RString.collection_section_items)) }
            items(visibleItems, key = { "item-${it.id}" }) { item ->
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
        } else if (uiState.items.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(PADDING),
                    text = stringResource(RString.collection_search_no_match, uiState.query.trim())
                )
            }
        }
    }
}

@Composable
private fun DeleteDialogs(uiState: CollectionUiState) {
    uiState.deleteSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = uiState.onDeleteDismiss,
            title = { Text(stringResource(RString.collection_delete_title)) },
            text = {
                val children =
                    pluralStringResource(RPlurals.collection_delete_children, summary.childCount, summary.childCount)
                val items = pluralStringResource(RPlurals.collection_delete_items, summary.itemCount, summary.itemCount)
                Text(stringResource(RString.collection_delete_text, uiState.title, children, items))
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
            title = { Text(stringResource(RString.collection_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.collection_delete_failed_ok))
                }
            }
        )
    }
}

/** The top bar's overflow menu. */
@Composable
private fun MoreMenu(onNewCollectionClick: () -> Unit, onEditClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(RString.collection_more))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(RString.collection_new)) },
                onClick = {
                    isExpanded = false
                    onNewCollectionClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(RString.collection_edit)) },
                onClick = {
                    isExpanded = false
                    onEditClick()
                }
            )
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

private val PROGRESS_SIZE = 24.dp
