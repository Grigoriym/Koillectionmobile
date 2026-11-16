package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_add_field
import com.grappim.koillectionmobile.strings.generated.resources.add_item_back
import com.grappim.koillectionmobile.strings.generated.resources.add_item_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.add_item_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.add_item_main_image
import com.grappim.koillectionmobile.strings.generated.resources.add_item_make_main
import com.grappim.koillectionmobile.strings.generated.resources.add_item_name_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_photo_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_pick_photos
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.add_item_section_photos
import com.grappim.koillectionmobile.strings.generated.resources.add_item_take_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onOpenItem] gets the new item's id once the save is done, to open it. */
@Composable
fun AddItemScreen(
    collectionId: String,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddItemViewModel = koinViewModel { parametersOf(collectionId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onOpenItem)
    LaunchedEffect(uiState.savedItemId) {
        uiState.savedItemId?.let { currentOnSaved(it) }
    }
    AddItemContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemContent(uiState: AddItemUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    // One back tap must not lose a filled form (M3.7): the arrow and the system back ask first.
    var isDiscardShown by rememberSaveable { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.hasInput,
        onBackCompleted = { isDiscardShown = true }
    )
    if (isDiscardShown) {
        DiscardDialog(
            title = RString.add_item_discard_title,
            text = RString.add_item_discard_text,
            onDiscard = {
                isDiscardShown = false
                onBack()
            },
            onDismiss = { isDiscardShown = false }
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RString.add_item_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.hasInput) isDiscardShown = true else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(RString.add_item_back)
                        )
                    }
                },
                actions = {
                    if (uiState.save == null) {
                        TextButton(onClick = uiState.onSave, enabled = uiState.canSave) {
                            Text(stringResource(RString.add_item_save))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val save = uiState.save
        if (save != null) {
            SaveView(
                status = save,
                partialText = RString.add_item_save_partial,
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@Scaffold
        }
        val duplicates = uiState.duplicateLabels
        val photoSources = rememberPhotoSources(onPhotos = uiState.onPhotosAdd)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(PADDING)
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = uiState.onNameChange,
                    label = { Text(stringResource(RString.add_item_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PADDING)
                        .padding(top = PADDING)
                )
            }
            item(key = "photos-title") {
                Text(
                    text = stringResource(RString.add_item_section_photos),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = PADDING)
                )
            }
            itemsIndexed(uiState.photos, key = { _, photo -> "photo-${photo.id}" }) { index, photo ->
                PhotoRowEditor(
                    photo = photo,
                    isMain = index == 0,
                    labelError = labelError(photo.label, duplicates),
                    uiState = uiState,
                    modifier = Modifier.padding(horizontal = PADDING)
                )
            }
            if (photoSources != null) {
                item(key = "add-photo") {
                    Row(
                        modifier = Modifier.padding(horizontal = PADDING),
                        horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)
                    ) {
                        OutlinedButton(onClick = photoSources.takePhoto) {
                            Text(stringResource(RString.add_item_take_photo))
                        }
                        OutlinedButton(onClick = photoSources.pickPhotos) {
                            Text(stringResource(RString.add_item_pick_photos))
                        }
                    }
                }
            }
            items(uiState.fields, key = { "field-${it.id}" }) { row ->
                FieldRowEditor(
                    label = row.label,
                    value = row.value,
                    labelError = labelError(row.label, duplicates),
                    onLabelChange = { uiState.onLabelChange(row.id, it) },
                    onValueChange = { uiState.onValueChange(row.id, it) },
                    onRemove = { uiState.onRemoveField(row.id) },
                    modifier = Modifier.padding(horizontal = PADDING)
                )
            }
            item(key = "add-field") {
                OutlinedButton(onClick = uiState.onAddField, modifier = Modifier.padding(horizontal = PADDING)) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(RString.add_item_add_field))
                }
            }
            item(key = "tags") {
                TagPicker(
                    tagGroups = uiState.tagGroups,
                    selectedTagIds = uiState.selectedTagIds,
                    isLoading = uiState.isLoadingTags,
                    error = uiState.tagsError,
                    onTagToggle = uiState.onTagToggle,
                    onRetry = uiState.onRetryTags,
                    modifier = Modifier
                        .padding(horizontal = PADDING)
                        .padding(bottom = PADDING)
                )
            }
        }
    }
}

/**
 * One photo: the preview, then the main-image title or the image field's label, and the photo's
 * buttons. Only an image field has a "make main" button.
 */
@Composable
private fun PhotoRowEditor(
    photo: PhotoRow,
    isMain: Boolean,
    labelError: StringResource?,
    uiState: AddItemUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)
    ) {
        PhotoPreview(uri = photo.uri, rotation = photo.rotation, modifier = Modifier.size(PREVIEW_SIZE))
        Column(modifier = Modifier.weight(1f)) {
            if (isMain) {
                Text(text = stringResource(RString.add_item_main_image), style = MaterialTheme.typography.labelLarge)
            } else {
                OutlinedTextField(
                    value = photo.label,
                    onValueChange = { uiState.onPhotoLabelChange(photo.id, it) },
                    label = { Text(stringResource(RString.add_item_photo_label)) },
                    singleLine = true,
                    isError = labelError != null,
                    supportingText = labelError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row {
                IconButton(onClick = { uiState.onPhotoRotate(photo.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(RString.add_item_rotate_photo)
                    )
                }
                if (!isMain) {
                    IconButton(onClick = { uiState.onPhotoMakeMain(photo.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(RString.add_item_make_main)
                        )
                    }
                }
                IconButton(onClick = { uiState.onPhotoRemove(photo.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(RString.add_item_remove_photo)
                    )
                }
            }
        }
    }
}

private val PADDING = 16.dp
private val TAG_SPACING = 8.dp
private val PREVIEW_SIZE = 120.dp
