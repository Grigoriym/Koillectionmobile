package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionMenu
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarMenuItem
import com.grappim.koillectionmobile.core.ui.DeleteDialogs
import com.grappim.koillectionmobile.core.ui.DeleteTexts
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.strings.RPlurals
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_add_item
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_children
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
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
        modifier = modifier
    )
}

@Composable
private fun CollectionContent(
    uiState: CollectionUiState,
    onCollectionClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onNewCollectionClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarController = LocalTopBarConfig.current
    val deleteLabel = stringResource(RString.collection_delete)
    val moreLabel = stringResource(RString.collection_more)
    val currentOnDeleteClick by rememberUpdatedState(uiState.onDeleteClick)
    val currentOnNewCollectionClick by rememberUpdatedState(onNewCollectionClick)
    val currentOnEditClick by rememberUpdatedState(onEditClick)
    // The kit bar has no progress slot: while the delete runs, Delete stays, disabled, and the
    // progress shows under the bar.
    val isBusy = uiState.isCountingDelete || uiState.isDeleting
    val hasDelete = uiState.canDelete || isBusy
    val hasMenu = !uiState.isLoading && uiState.error.isEmpty() && !uiState.isDeleted
    LaunchedEffect(uiState.title, hasDelete, isBusy, hasMenu, deleteLabel, moreLabel) {
        val delete = TopBarActionVectorButton(
            imageVector = Icons.Filled.Delete,
            contentDescription = deleteLabel,
            enabled = !isBusy,
            onClick = { currentOnDeleteClick() }
        )
        val menu = TopBarActionMenu(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = moreLabel,
            items = persistentListOf(
                TopBarMenuItem(
                    text = NativeText.Resource(RString.collection_new),
                    onClick = { currentOnNewCollectionClick() }
                ),
                TopBarMenuItem(
                    text = NativeText.Resource(RString.collection_edit),
                    onClick = { currentOnEditClick() }
                )
            )
        )
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(uiState.title),
                navigationIcon = NavigationIconConfig.Back(),
                actions = listOfNotNull(delete.takeIf { hasDelete }, menu.takeIf { hasMenu }).toPersistentList()
            )
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                onRetry = uiState.onRetry
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
    DeleteDialogs(
        summary = uiState.deleteSummary,
        error = uiState.deleteError,
        name = uiState.title,
        texts = DeleteTexts(
            title = RString.collection_delete_title,
            text = RString.collection_delete_text,
            children = RPlurals.collection_delete_children,
            contents = RPlurals.collection_delete_items,
            failedTitle = RString.collection_delete_failed_title
        ),
        onConfirm = uiState.onDeleteConfirm,
        onDismiss = uiState.onDeleteDismiss,
        onErrorDismiss = uiState.onDeleteErrorShown
    )
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
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(PADDING),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
