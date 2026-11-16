package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.core.photos.PhotoPreview
import com.grappim.koillectionmobile.core.photos.rememberPhotoSources
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_back
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_form
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_pick_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_preparing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_remove_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_retry
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_section_image
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_sending
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_take_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_title_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The new-collection form, in [parentId] or at the top level when it is null. [onOpenCollection]
 * runs once the collection is saved, with its id.
 */
@Composable
fun NewCollectionScreen(
    parentId: String?,
    onBack: () -> Unit,
    onOpenCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewCollectionViewModel = koinViewModel { parametersOf(parentId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpen by rememberUpdatedState(onOpenCollection)
    LaunchedEffect(uiState.savedCollectionId) {
        uiState.savedCollectionId?.let { currentOnOpen(it) }
    }
    NewCollectionContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewCollectionContent(uiState: NewCollectionUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    // One back tap must not lose a filled form, as on the add-item form: the arrow and the system
    // back ask first.
    var isDiscardShown by rememberSaveable { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.hasInput,
        onBackCompleted = { isDiscardShown = true }
    )
    if (isDiscardShown) {
        AlertDialog(
            onDismissRequest = { isDiscardShown = false },
            title = { Text(stringResource(RString.collection_form_discard_title)) },
            text = { Text(stringResource(RString.collection_form_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    isDiscardShown = false
                    onBack()
                }) { Text(stringResource(RString.collection_form_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { isDiscardShown = false }) {
                    Text(stringResource(RString.collection_form_keep_editing))
                }
            }
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RString.collection_form_new_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.hasInput) isDiscardShown = true else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(RString.collection_form_back)
                        )
                    }
                },
                actions = {
                    if (uiState.save == null) {
                        TextButton(onClick = uiState.onSave, enabled = uiState.canSave) {
                            Text(stringResource(RString.collection_form_save))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        val save = uiState.save
        if (save == null) {
            CollectionForm(uiState = uiState, modifier = contentModifier)
        } else {
            CollectionSaveView(
                status = save,
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun CollectionForm(uiState: NewCollectionUiState, modifier: Modifier = Modifier) {
    val photoSources = rememberPhotoSources(onPhotos = uiState.onPhotosAdd)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = uiState.onTitleChange,
            label = { Text(stringResource(RString.collection_form_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(RString.collection_form_section_image),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        val photo = uiState.photo
        if (photo != null) {
            PhotoPreview(uri = photo.uri, rotation = photo.rotation, modifier = Modifier.size(PREVIEW_SIZE))
            Row(horizontalArrangement = Arrangement.spacedBy(PADDING / 2)) {
                OutlinedButton(onClick = uiState.onPhotoRotate) {
                    Text(stringResource(RString.collection_form_rotate_photo))
                }
                OutlinedButton(onClick = uiState.onPhotoRemove) {
                    Text(stringResource(RString.collection_form_remove_photo))
                }
            }
        }
        if (photoSources != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(PADDING / 2)) {
                OutlinedButton(onClick = photoSources.takePhoto) {
                    Text(stringResource(RString.collection_form_take_photo))
                }
                OutlinedButton(onClick = photoSources.pickPhotos) {
                    Text(stringResource(RString.collection_form_pick_photo))
                }
            }
        }
    }
}

/**
 * The save instead of the form, as on the add-item form: a progress bar while it runs; on a
 * failure the message and a retry. The way back to the form is offered only while nothing exists
 * on the server.
 */
@Composable
private fun CollectionSaveView(
    status: CollectionSaveStatus,
    onRetry: () -> Unit,
    onEditForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            CollectionSaveStatus.Preparing -> {
                CircularProgressIndicator()
                Text(stringResource(RString.collection_form_preparing))
            }

            is CollectionSaveStatus.Sending -> {
                LinearProgressIndicator(
                    progress = { status.done.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(RString.collection_form_sending, status.done, status.total))
            }

            is CollectionSaveStatus.Failed -> {
                Text(text = status.message.asString(), color = MaterialTheme.colorScheme.error)
                if (status.isPartial) Text(stringResource(RString.collection_form_save_partial))
                Button(onClick = onRetry) { Text(stringResource(RString.collection_form_retry)) }
                if (!status.isPartial) {
                    OutlinedButton(onClick = onEditForm) { Text(stringResource(RString.collection_form_edit_form)) }
                }
            }
        }
    }
}

private val PREVIEW_SIZE = 160.dp
