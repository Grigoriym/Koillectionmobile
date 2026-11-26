package com.grappim.koillectionmobile.feature.inventories.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionMenu
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarMenuItem
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryCollection
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.inventory_collection_count
import com.grappim.koillectionmobile.strings.generated.resources.inventory_collection_hide
import com.grappim.koillectionmobile.strings.generated.resources.inventory_collection_show
import com.grappim.koillectionmobile.strings.generated.resources.inventory_empty
import com.grappim.koillectionmobile.strings.generated.resources.inventory_more
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One inventory as a tree: each collection with its checked / total count, each item with a
 * read-only checked mark. A tap on a collection opens or closes it. [onClose] runs once the server
 * deleted the inventory, to take the screen off the back stack.
 */
@Composable
fun InventoryScreen(
    inventoryId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = koinViewModel { parametersOf(inventoryId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    InventoryContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun InventoryContent(uiState: InventoryUiState, modifier: Modifier = Modifier) {
    val topBarController = LocalTopBarConfig.current
    val moreLabel = stringResource(RString.inventory_more)
    val currentOnDeleteClick by rememberUpdatedState(uiState.onDeleteClick)
    // While the delete runs, the menu stays and its Delete item is disabled.
    val hasMenu = uiState.canDelete || uiState.isDeleting
    LaunchedEffect(uiState.name, hasMenu, uiState.canDelete, moreLabel) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(uiState.name),
                navigationIcon = NavigationIconConfig.Back(),
                actions = if (hasMenu) {
                    persistentListOf(
                        TopBarActionMenu(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = moreLabel,
                            items = persistentListOf(
                                TopBarMenuItem(
                                    text = NativeText.Resource(RString.inventories_delete),
                                    enabled = uiState.canDelete,
                                    onClick = { currentOnDeleteClick() }
                                )
                            )
                        )
                    )
                } else {
                    persistentListOf()
                }
            )
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.isDeleting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.collections.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.inventory_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.rows, key = { it.key }) { row ->
                    when (row) {
                        is InventoryRow.Collection -> CollectionHeader(
                            collection = row.collection,
                            depth = row.depth,
                            isExpanded = row.isExpanded,
                            showDivider = row.depth == 0 && row.collection.id != uiState.collections.first().id,
                            onClick = { uiState.onToggle(row.collection.id) },
                            modifier = Modifier.animateItem()
                        )

                        is InventoryRow.Item -> Row(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(start = indent(row.depth), end = PADDING),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Read only: the web UI ticks items, the app shows what the server stores.
                            Checkbox(
                                checked = row.item.checked,
                                onCheckedChange = null,
                                modifier = Modifier.padding(12.dp)
                            )
                            Text(text = row.item.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
    DeleteDialogs(uiState)
}

/** A full-width row that opens or closes the collection: title, checked / total and an arrow. */
@Composable
private fun CollectionHeader(
    collection: InventoryCollection,
    depth: Int,
    isExpanded: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) HALF_TURN else 0f)
    Column(modifier = modifier) {
        if (showDivider) HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = indent(depth), end = PADDING, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = collection.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    RString.inventory_collection_count,
                    collection.checkedItems,
                    collection.totalItems
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                modifier = Modifier.rotate(arrowRotation),
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (isExpanded) RString.inventory_collection_hide else RString.inventory_collection_show
                )
            )
        }
    }
}

@Composable
private fun DeleteDialogs(uiState: InventoryUiState) {
    if (uiState.isDeleteConfirm) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteDismiss,
            title = { Text(stringResource(RString.inventories_delete_title)) },
            text = { Text(stringResource(RString.inventories_delete_text, uiState.name)) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteConfirm) {
                    Text(stringResource(RString.inventories_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = uiState.onDeleteDismiss) {
                    Text(stringResource(RString.inventories_delete_cancel))
                }
            }
        )
    }
    if (uiState.deleteError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteErrorShown,
            title = { Text(stringResource(RString.inventories_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.inventories_delete_failed_ok))
                }
            }
        )
    }
}

/** The start padding of a row at [depth]: one step per level under the root. */
private fun indent(depth: Int): Dp = PADDING + INDENT_STEP * depth

private val PADDING = 16.dp
private val INDENT_STEP = 16.dp
private const val HALF_TURN = 180f
