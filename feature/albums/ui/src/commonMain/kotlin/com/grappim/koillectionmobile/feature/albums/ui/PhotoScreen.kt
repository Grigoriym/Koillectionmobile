package com.grappim.koillectionmobile.feature.albums.ui

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
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.photo_delete
import com.grappim.koillectionmobile.strings.generated.resources.photo_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.photo_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.photo_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.photo_edit
import com.grappim.koillectionmobile.strings.generated.resources.photo_label_comment
import com.grappim.koillectionmobile.strings.generated.resources.photo_label_place
import com.grappim.koillectionmobile.strings.generated.resources.photo_label_taken_at
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onClose] runs once the server deleted the photo, to take the screen off the back stack. */
@Composable
fun PhotoScreen(
    photoId: String,
    onEditClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoViewModel = koinViewModel { parametersOf(photoId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    PhotoContent(uiState = uiState, onEditClick = onEditClick, modifier = modifier)
}

@Composable
private fun PhotoContent(uiState: PhotoUiState, onEditClick: () -> Unit, modifier: Modifier = Modifier) {
    var isDeleteConfirmShown by rememberSaveable { mutableStateOf(false) }
    val title = uiState.photo?.title.orEmpty()
    val topBarController = LocalTopBarConfig.current
    val editLabel = stringResource(RString.photo_edit)
    val deleteLabel = stringResource(RString.photo_delete)
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
        val photo = uiState.photo
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            photo == null -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = "",
                onRetry = uiState.onRetry
            )

            else -> PhotoDetails(photo = photo, modifier = contentModifier)
        }
    }

    if (isDeleteConfirmShown) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmShown = false },
            title = { Text(stringResource(RString.photo_delete_title)) },
            text = { Text(stringResource(RString.photo_delete_text, uiState.photo?.title.orEmpty())) },
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
            title = { Text(stringResource(RString.photo_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.collection_delete_failed_ok))
                }
            }
        )
    }
}

/** The image, then each field the photo has. The title is in the top bar. */
@Composable
private fun PhotoDetails(photo: Photo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        photo.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(top = PADDING),
                contentScale = ContentScale.FillWidth
            )
        }
        photo.comment?.let { Field(label = stringResource(RString.photo_label_comment), value = it) }
        photo.place?.let { Field(label = stringResource(RString.photo_label_place), value = it) }
        photo.takenAt?.let { Field(label = stringResource(RString.photo_label_taken_at), value = it.toString()) }
    }
}

/** A labelled value. The value is selectable, so the user can copy it. */
@Composable
private fun Field(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PADDING)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        SelectionContainer {
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
