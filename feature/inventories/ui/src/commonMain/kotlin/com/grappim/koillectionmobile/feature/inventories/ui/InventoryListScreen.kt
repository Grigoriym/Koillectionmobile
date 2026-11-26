package com.grappim.koillectionmobile.feature.inventories.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.inventories_checked
import com.grappim.koillectionmobile.strings.generated.resources.inventories_created_at
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.inventories_empty
import com.grappim.koillectionmobile.strings.generated.resources.inventories_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The root of the Inventories section. Each row shows the name, the created date as `yyyy-MM-dd`
 * and the checked / total items, with a delete action that asks first. A tap on a row opens the
 * inventory.
 */
@Composable
fun InventoryListScreen(
    onInventoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarController = LocalTopBarConfig.current
    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.inventories_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }
    InventoryListContent(uiState = uiState, onInventoryClick = onInventoryClick, modifier = modifier)
}

@Composable
private fun InventoryListContent(
    uiState: InventoryListUiState,
    onInventoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentModifier = modifier.fillMaxSize()
    when {
        uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.error.isNotEmpty() || uiState.inventories.isEmpty() -> EmptyOrError(
            modifier = contentModifier,
            error = uiState.error,
            emptyText = stringResource(RString.inventories_empty),
            onRetry = uiState.onRetry
        )

        else -> LazyColumn(modifier = contentModifier) {
            items(uiState.inventories, key = { it.id }) { inventory ->
                InventoryRow(
                    inventory = inventory,
                    isDeleting = uiState.deletingId == inventory.id,
                    isEnabled = uiState.deletingId == null,
                    onClick = { onInventoryClick(inventory.id) },
                    onDelete = { uiState.onDeleteClick(inventory.id) }
                )
            }
        }
    }
    DeleteDialogs(uiState)
}

@Composable
private fun InventoryRow(
    inventory: Inventory,
    isDeleting: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(inventory.name) },
        supportingContent = {
            Column {
                Text(stringResource(RString.inventories_created_at, inventory.createdAt.toString()))
                Text(stringResource(RString.inventories_checked, inventory.checkedItems, inventory.totalItems))
            }
        },
        trailingContent = {
            if (isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE))
            } else {
                IconButton(onClick = onDelete, enabled = isEnabled) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(RString.inventories_delete)
                    )
                }
            }
        }
    )
}

@Composable
private fun DeleteDialogs(uiState: InventoryListUiState) {
    uiState.deleteConfirm?.let { inventory ->
        AlertDialog(
            onDismissRequest = uiState.onDeleteDismiss,
            title = { Text(stringResource(RString.inventories_delete_title)) },
            text = { Text(stringResource(RString.inventories_delete_text, inventory.name)) },
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

private val PROGRESS_SIZE = 24.dp
