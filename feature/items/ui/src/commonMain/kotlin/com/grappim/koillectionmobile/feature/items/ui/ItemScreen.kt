package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.item_delete
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.item_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.item_edit
import com.grappim.koillectionmobile.strings.generated.resources.item_field_not_shown
import com.grappim.koillectionmobile.strings.generated.resources.item_image_full
import com.grappim.koillectionmobile.strings.generated.resources.item_retry
import com.grappim.koillectionmobile.strings.generated.resources.item_section_tags
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [onClose] runs once the server deleted the item, to take the screen off the back stack.
 * [onEdit] opens the edit form. [loans] is the loans section (`feature:loans:ui`), shown below the
 * tags once the item is loaded. The shell passes it, so this module does not depend on the loans.
 */
@Composable
fun ItemScreen(
    itemId: String,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    loans: @Composable () -> Unit = {},
    viewModel: ItemViewModel = koinViewModel { parametersOf(itemId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    ItemContent(uiState = uiState, onEdit = onEdit, loans = loans, modifier = modifier)
}

@Composable
private fun ItemContent(
    uiState: ItemUiState,
    onEdit: () -> Unit,
    loans: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // The full image on screen, or null. Kept across rotation.
    var fullImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isDeleteConfirmShown by rememberSaveable { mutableStateOf(false) }

    val topBarController = LocalTopBarConfig.current
    val editLabel = stringResource(RString.item_edit)
    val deleteLabel = stringResource(RString.item_delete)
    val currentOnEdit by rememberUpdatedState(onEdit)
    // The kit bar has no progress slot: while the delete runs, Delete stays, disabled, and the
    // progress shows under the bar.
    val hasDelete = uiState.canDelete || uiState.isDeleting
    LaunchedEffect(uiState.name, uiState.canEdit, hasDelete, uiState.isDeleting, editLabel, deleteLabel) {
        val edit = TopBarActionVectorButton(
            imageVector = Icons.Filled.Edit,
            contentDescription = editLabel,
            onClick = { currentOnEdit() }
        )
        val delete = TopBarActionVectorButton(
            imageVector = Icons.Filled.Delete,
            contentDescription = deleteLabel,
            enabled = !uiState.isDeleting,
            onClick = { isDeleteConfirmShown = true }
        )
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(uiState.name),
                navigationIcon = NavigationIconConfig.Back(),
                actions = listOfNotNull(edit.takeIf { uiState.canEdit }, delete.takeIf { hasDelete }).toPersistentList()
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

            uiState.error.isNotEmpty() -> Column(
                modifier = contentModifier.padding(PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically)
            ) {
                Text(
                    text = uiState.error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = uiState.onRetry) { Text(stringResource(RString.item_retry)) }
            }

            else -> ItemDetails(
                uiState = uiState,
                onImageClick = { fullImageUrl = it },
                loans = loans,
                modifier = contentModifier
            )
        }
    }

    fullImageUrl?.let { url -> FullImage(url = url, onDismiss = { fullImageUrl = null }) }
    if (isDeleteConfirmShown) {
        DeleteDialog(
            name = uiState.name,
            onDelete = {
                isDeleteConfirmShown = false
                uiState.onDelete()
            },
            onDismiss = { isDeleteConfirmShown = false }
        )
    }
    if (uiState.deleteError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteErrorShown,
            title = { Text(stringResource(RString.item_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.item_delete_failed_ok))
                }
            }
        )
    }
}

@Composable
private fun DeleteDialog(name: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.item_delete_title)) },
        text = { Text(stringResource(RString.item_delete_text, name)) },
        confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(RString.item_delete_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(RString.item_delete_cancel)) } }
    )
}

@Composable
private fun ItemDetails(
    uiState: ItemUiState,
    onImageClick: (String) -> Unit,
    loans: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        uiState.image?.let { image ->
            item(key = "image") {
                ItemImage(image = image, onClick = onImageClick, modifier = Modifier.padding(top = PADDING))
            }
        }
        items(uiState.fields, key = { it.id }) { field ->
            Field(field = field, onImageClick = onImageClick, modifier = Modifier.padding(horizontal = PADDING))
        }
        if (uiState.tags.isNotEmpty()) {
            item(key = "tags") {
                Column(modifier = Modifier.padding(horizontal = PADDING).padding(bottom = PADDING)) {
                    Label(stringResource(RString.item_section_tags))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
                        uiState.tags.forEach { tag -> SuggestionChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
            }
        }
        item(key = "loans") {
            Box(modifier = Modifier.padding(horizontal = PADDING).padding(bottom = PADDING)) { loans() }
        }
    }
}

@Composable
private fun Field(field: ItemField, onImageClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Label(field.label)
        when (field) {
            is ItemField.Text -> Text(text = field.value, style = MaterialTheme.typography.bodyLarge)

            is ItemField.Picture -> ItemImage(image = field.image, onClick = onImageClick)

            is ItemField.NotShown -> Text(
                text = stringResource(RString.item_field_not_shown, field.type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** The large thumbnail, or the full image when the server sent no thumbnail. A tap opens the full image. */
@Composable
private fun ItemImage(image: Image, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    AsyncImage(
        model = image.largeThumbnailUrl ?: image.url,
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(image.url) },
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun FullImage(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(RString.item_image_full),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private val PADDING = 16.dp
private val TAG_SPACING = 8.dp
