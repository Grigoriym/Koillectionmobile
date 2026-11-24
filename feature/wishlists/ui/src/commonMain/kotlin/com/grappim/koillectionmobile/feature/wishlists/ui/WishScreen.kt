package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.wish_delete
import com.grappim.koillectionmobile.strings.generated.resources.wish_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.wish_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.wish_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.wish_edit
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_comment
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_price
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_url
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onClose] runs once the server deleted the wish, to take the screen off the back stack. */
@Composable
fun WishScreen(
    wishId: String,
    onEditClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishViewModel = koinViewModel { parametersOf(wishId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    WishContent(uiState = uiState, onEditClick = onEditClick, modifier = modifier)
}

@Composable
private fun WishContent(uiState: WishUiState, onEditClick: () -> Unit, modifier: Modifier = Modifier) {
    var isDeleteConfirmShown by rememberSaveable { mutableStateOf(false) }
    val title = uiState.wish?.name.orEmpty()
    val topBarController = LocalTopBarConfig.current
    val editLabel = stringResource(RString.wish_edit)
    val deleteLabel = stringResource(RString.wish_delete)
    val currentOnEditClick by rememberUpdatedState(onEditClick)
    // The kit bar has no progress slot: while the delete runs, the actions stay, disabled, and the
    // progress shows under the bar.
    val hasActions = uiState.canDelete || uiState.isDeleting
    LaunchedEffect(title, hasActions, uiState.isDeleting, editLabel, deleteLabel) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(title),
                navigationIcon = NavigationIconConfig.Back(),
                actions = if (hasActions) {
                    persistentListOf(
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = editLabel,
                            enabled = !uiState.isDeleting,
                            onClick = { currentOnEditClick() }
                        ),
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = deleteLabel,
                            enabled = !uiState.isDeleting,
                            onClick = { isDeleteConfirmShown = true }
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
        val wish = uiState.wish
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            wish == null -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = "",
                onRetry = uiState.onRetry
            )

            else -> WishDetails(wish = wish, modifier = contentModifier)
        }
    }

    if (isDeleteConfirmShown) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmShown = false },
            title = { Text(stringResource(RString.wish_delete_title)) },
            text = { Text(stringResource(RString.wish_delete_text, uiState.wish?.name.orEmpty())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteConfirmShown = false
                        uiState.onDelete()
                    }
                ) { Text(stringResource(RString.collection_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmShown = false }) {
                    Text(stringResource(RString.collection_delete_cancel))
                }
            }
        )
    }
    if (uiState.deleteError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteErrorShown,
            title = { Text(stringResource(RString.wish_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.collection_delete_failed_ok))
                }
            }
        )
    }
}

/** The image, then each field the wish has. The name is in the top bar. */
@Composable
private fun WishDetails(wish: Wish, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        wish.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(top = PADDING),
                contentScale = ContentScale.FillWidth
            )
        }
        wish.url?.let { Field(label = stringResource(RString.wish_label_url), value = it) }
        listOfNotNull(wish.price, wish.currency).takeIf { it.isNotEmpty() }?.let {
            Field(label = stringResource(RString.wish_label_price), value = it.joinToString(" "))
        }
        wish.comment?.let { Field(label = stringResource(RString.wish_label_comment), value = it) }
    }
}

/** A labelled value. The value is selectable, so the user can copy a link. */
@Composable
private fun Field(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PADDING)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        SelectionContainer {
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
