package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionTextButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.photos.PhotoPreview
import com.grappim.koillectionmobile.core.photos.rememberPhotoSources
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_add_field
import com.grappim.koillectionmobile.strings.generated.resources.add_item_main_image
import com.grappim.koillectionmobile.strings.generated.resources.add_item_name_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_photo_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_pick_photos
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_save
import com.grappim.koillectionmobile.strings.generated.resources.add_item_section_photos
import com.grappim.koillectionmobile.strings.generated.resources.add_item_take_photo
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_discard_text_partial
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_no_main_image
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_read_only
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_replace_photo
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.edit_item_title
import com.grappim.koillectionmobile.strings.generated.resources.item_retry
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onBack] also runs once the save is done: the item screen below reads the item again. */
@Composable
fun EditItemScreen(
    itemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditItemViewModel = koinViewModel { parametersOf(itemId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnBack()
    }
    EditItemContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@Composable
private fun EditItemContent(uiState: EditItemUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    // One back tap must not lose the edits, as on the add form (M3.7).
    var isDiscardShown by rememberSaveable { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.hasChanges,
        onBackCompleted = { isDiscardShown = true }
    )
    if (isDiscardShown) {
        DiscardDialog(
            title = RString.edit_item_discard_title,
            text = if ((uiState.save as? SaveStatus.Failed)?.isPartial == true) {
                RString.edit_item_discard_text_partial
            } else {
                RString.edit_item_discard_text
            },
            onDiscard = {
                isDiscardShown = false
                onBack()
            },
            onDismiss = { isDiscardShown = false }
        )
    }
    val topBarController = LocalTopBarConfig.current
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSave by rememberUpdatedState(uiState.onSave)
    val hasInput = uiState.hasChanges
    val isSaveShown = uiState.draft != null && uiState.save == null
    val canSave = uiState.canSave
    LaunchedEffect(hasInput, isSaveShown, canSave) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.edit_item_title),
                navigationIcon = NavigationIconConfig.Back(
                    onBackClick = { if (hasInput) isDiscardShown = true else currentOnBack() }
                ),
                actions = if (isSaveShown) {
                    persistentListOf(
                        TopBarActionTextButton(
                            text = NativeText.Resource(RString.add_item_save),
                            enabled = canSave,
                            onClick = { currentOnSave() }
                        )
                    )
                } else {
                    persistentListOf()
                }
            )
        )
    }
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        val draft = uiState.draft
        val save = uiState.save
        when {
            save != null -> SaveView(
                status = save,
                partialText = RString.edit_item_save_partial,
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )

            draft != null -> EditForm(draft = draft, uiState = uiState, modifier = contentModifier)

            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> Column(
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
        }
    }
}

@Composable
private fun EditForm(draft: EditItemDraft, uiState: EditItemUiState, modifier: Modifier = Modifier) {
    val duplicates = uiState.duplicateLabels
    val newPhotoSources = rememberPhotoSources(onPhotos = uiState.onPhotosAdd)
    // The photo a replace is for. Kept in saved state: the camera app can push this process out.
    var replaceTargetId by rememberSaveable { mutableStateOf<Int?>(null) }
    val replaceSources = rememberPhotoSources { uris -> replaceTargetId?.let { uiState.onPhotoReplace(it, uris) } }
    val onReplace: (Int, Boolean) -> Unit = { id, isCamera ->
        replaceTargetId = id
        replaceSources?.let { if (isCamera) it.takePhoto() else it.pickPhotos() }
    }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PADDING)) {
        item(key = "name") {
            OutlinedTextField(
                value = draft.name,
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
        item(key = "photo-${draft.mainImage.id}") {
            EditPhotoEditor(
                photo = draft.mainImage,
                isMain = true,
                labelError = null,
                canReplace = replaceSources != null,
                uiState = uiState,
                onReplace = onReplace,
                modifier = Modifier.padding(horizontal = PADDING)
            )
        }
        items(draft.photos, key = { "photo-${it.id}" }) { photo ->
            EditPhotoEditor(
                photo = photo,
                isMain = false,
                labelError = labelError(photo.label, duplicates),
                canReplace = replaceSources != null,
                uiState = uiState,
                onReplace = onReplace,
                modifier = Modifier.padding(horizontal = PADDING)
            )
        }
        if (newPhotoSources != null) {
            item(key = "add-photo") {
                Row(
                    modifier = Modifier.padding(horizontal = PADDING),
                    horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)
                ) {
                    OutlinedButton(onClick = newPhotoSources.takePhoto) {
                        Text(stringResource(RString.add_item_take_photo))
                    }
                    OutlinedButton(onClick = newPhotoSources.pickPhotos) {
                        Text(stringResource(RString.add_item_pick_photos))
                    }
                }
            }
        }
        items(draft.fields, key = { "field-${it.id}" }) { row ->
            val rowModifier = Modifier.padding(horizontal = PADDING)
            when (row) {
                is EditRow.Text -> FieldRowEditor(
                    label = row.label,
                    value = row.value,
                    type = row.datumType,
                    choiceListId = row.choiceListId,
                    labelError = labelError(row.label, duplicates),
                    canChangeType = row.datumId == null,
                    choiceLists = uiState.choiceLists,
                    actions = FieldRowActions(
                        onLabelChange = { uiState.onLabelChange(row.id, it) },
                        onValueChange = { uiState.onValueChange(row.id, it) },
                        onTypeChange = { uiState.onTypeChange(row.id, it) },
                        onChoiceListChange = { uiState.onChoiceListChange(row.id, it) },
                        onCheckedChange = { uiState.onCheckedChange(row.id, it) },
                        onChoiceToggle = { uiState.onChoiceToggle(row.id, it) },
                        onRetryChoiceLists = uiState.onRetryChoiceLists,
                        onRemove = { uiState.onRemoveField(row.id) }
                    ),
                    modifier = rowModifier
                )

                is EditRow.ReadOnly -> ReadOnlyField(row = row, modifier = rowModifier)
            }
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
                selectedTagIds = draft.selectedTagIds,
                newTagLabels = draft.newTagLabels,
                isLoading = uiState.isLoadingTags,
                error = uiState.tagsError,
                onTagToggle = uiState.onTagToggle,
                onNewTag = uiState.onNewTag,
                onNewTagRemove = uiState.onNewTagRemove,
                onRetry = uiState.onRetryTags,
                modifier = Modifier
                    .padding(horizontal = PADDING)
                    .padding(bottom = PADDING)
            )
        }
    }
}

@Composable
private fun ReadOnlyField(row: EditRow.ReadOnly, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = row.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        row.value?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        Text(
            text = stringResource(RString.edit_item_read_only, row.datumType),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One image: the preview (the photo from the device, or else the server's image), then the
 * main-image title or the image field's label, and the buttons. The main image has no remove;
 * only a photo from the device has a rotate. [onReplace] gets the photo id and true for the camera.
 */
@Composable
private fun EditPhotoEditor(
    photo: EditPhoto,
    isMain: Boolean,
    labelError: StringResource?,
    canReplace: Boolean,
    uiState: EditItemUiState,
    onReplace: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)
    ) {
        val previewModifier = Modifier.size(PREVIEW_SIZE)
        when {
            photo.uri != null -> PhotoPreview(uri = photo.uri, rotation = photo.rotation, modifier = previewModifier)

            photo.previewUrl != null -> AsyncImage(
                model = photo.previewUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = previewModifier
            )

            else -> Box(modifier = previewModifier)
        }
        Column(modifier = Modifier.weight(1f)) {
            if (isMain) {
                Text(text = stringResource(RString.add_item_main_image), style = MaterialTheme.typography.labelLarge)
                if (photo.uri == null && photo.previewUrl == null) {
                    Text(
                        text = stringResource(RString.edit_item_no_main_image),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canReplace) ReplaceButton(onReplace = { isCamera -> onReplace(photo.id, isCamera) })
                if (photo.uri != null) {
                    IconButton(onClick = { uiState.onPhotoRotate(photo.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(RString.add_item_rotate_photo)
                        )
                    }
                }
                if (!isMain) {
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
}

/** "Replace", then the camera or the picker. [onReplace] gets true for the camera. */
@Composable
private fun ReplaceButton(onReplace: (Boolean) -> Unit) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }
    Box {
        TextButton(onClick = { isMenuShown = true }) { Text(stringResource(RString.edit_item_replace_photo)) }
        DropdownMenu(expanded = isMenuShown, onDismissRequest = { isMenuShown = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(RString.add_item_take_photo)) },
                onClick = {
                    isMenuShown = false
                    onReplace(true)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(RString.add_item_pick_photos)) },
                onClick = {
                    isMenuShown = false
                    onReplace(false)
                }
            )
        }
    }
}

private val PADDING = 16.dp
private val TAG_SPACING = 8.dp
private val PREVIEW_SIZE = 120.dp
