package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_add_field
import com.grappim.koillectionmobile.strings.generated.resources.add_item_back
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_value
import com.grappim.koillectionmobile.strings.generated.resources.add_item_label_duplicate
import com.grappim.koillectionmobile.strings.generated.resources.add_item_main_image
import com.grappim.koillectionmobile.strings.generated.resources.add_item_make_main
import com.grappim.koillectionmobile.strings.generated.resources.add_item_name_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_category
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_tags
import com.grappim.koillectionmobile.strings.generated.resources.add_item_photo_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_pick_photos
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_field
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_retry
import com.grappim.koillectionmobile.strings.generated.resources.add_item_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_section_photos
import com.grappim.koillectionmobile.strings.generated.resources.add_item_section_tags
import com.grappim.koillectionmobile.strings.generated.resources.add_item_take_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddItemScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: AddItemViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddItemContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemContent(uiState: AddItemUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RString.add_item_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(RString.add_item_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
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
                    isDuplicate = index > 0 && photo.label in duplicates,
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
                    row = row,
                    isDuplicate = row.label in duplicates,
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
                    uiState = uiState,
                    modifier = Modifier
                        .padding(horizontal = PADDING)
                        .padding(bottom = PADDING)
                )
            }
        }
    }
}

@Composable
private fun FieldRowEditor(
    row: FieldRow,
    isDuplicate: Boolean,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = row.label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(RString.add_item_field_label)) },
                singleLine = true,
                isError = isDuplicate,
                supportingText = if (isDuplicate) {
                    { Text(stringResource(RString.add_item_label_duplicate)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = row.value,
                onValueChange = onValueChange,
                label = { Text(stringResource(RString.add_item_field_value)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(RString.add_item_remove_field))
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
    isDuplicate: Boolean,
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
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text(stringResource(RString.add_item_label_duplicate)) }
                    } else {
                        null
                    },
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

@Composable
private fun TagPicker(uiState: AddItemUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
        Text(
            text = stringResource(RString.add_item_section_tags),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        when {
            uiState.isLoadingTags -> CircularProgressIndicator()

            uiState.tagsError.isNotEmpty() -> {
                Text(text = uiState.tagsError.asString(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = uiState.onRetryTags) { Text(stringResource(RString.add_item_retry)) }
            }

            uiState.tagGroups.isEmpty() -> Text(stringResource(RString.add_item_no_tags))

            else -> uiState.tagGroups.forEach { group ->
                Text(
                    text = group.label ?: stringResource(RString.add_item_no_category),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
                    group.tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in uiState.selectedTagIds,
                            onClick = { uiState.onTagToggle(tag.id) },
                            label = { Text(tag.label) }
                        )
                    }
                }
            }
        }
    }
}

private val PADDING = 16.dp
private val TAG_SPACING = 8.dp
private val PREVIEW_SIZE = 120.dp
