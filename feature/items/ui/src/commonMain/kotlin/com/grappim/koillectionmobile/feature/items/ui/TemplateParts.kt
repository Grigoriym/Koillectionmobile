package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.core.photos.PhotoPreview
import com.grappim.koillectionmobile.core.photos.PhotoSources
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_templates
import com.grappim.koillectionmobile.strings.generated.resources.add_item_photo_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_pick_photo_one
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_slot
import com.grappim.koillectionmobile.strings.generated.resources.add_item_retry
import com.grappim.koillectionmobile.strings.generated.resources.add_item_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_skipped_fields
import com.grappim.koillectionmobile.strings.generated.resources.add_item_skipped_ok
import com.grappim.koillectionmobile.strings.generated.resources.add_item_slot_empty
import com.grappim.koillectionmobile.strings.generated.resources.add_item_take_photo
import com.grappim.koillectionmobile.strings.generated.resources.add_item_templates_cancel
import com.grappim.koillectionmobile.strings.generated.resources.add_item_templates_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The templates of the server; a tap on one applies it. */
@Composable
internal fun TemplatePickerDialog(
    picker: TemplatePickerUi,
    onPick: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.add_item_templates_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when {
                    picker.isLoading -> CircularProgressIndicator()

                    picker.error.isNotEmpty() -> {
                        Text(text = picker.error.asString(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text(stringResource(RString.add_item_retry)) }
                    }

                    picker.templates.isEmpty() -> Text(stringResource(RString.add_item_no_templates))

                    else -> picker.templates.forEach { template ->
                        Text(
                            text = template.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(template.id) }
                                .padding(vertical = ROW_PADDING)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.add_item_templates_cancel)) }
        }
    )
}

/** Names the template fields the form did not add, with their type. */
@Composable
internal fun SkippedFieldsNote(skipped: List<TemplateField>, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(
                RString.add_item_skipped_fields,
                skipped.joinToString { "${it.name} (${it.type})" }
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = onDismiss) { Text(stringResource(RString.add_item_skipped_ok)) }
    }
}

/**
 * An image slot: its label, then the photo with rotate, or the buttons that take or pick one.
 * [sources] is null where the platform has none; [onSourceUse] gets the slot's id before a source starts.
 */
@Composable
internal fun ImageSlotEditor(
    slot: ImageSlot,
    labelError: StringResource?,
    uiState: AddItemUiState,
    sources: PhotoSources?,
    onSourceUse: (Int) -> Unit,
    previewSize: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(ROW_PADDING)
    ) {
        val uri = slot.uri
        if (uri != null) PhotoPreview(uri = uri, rotation = slot.rotation, modifier = Modifier.size(previewSize))
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = slot.label,
                onValueChange = { uiState.onSlotLabelChange(slot.id, it) },
                label = { Text(stringResource(RString.add_item_photo_label)) },
                singleLine = true,
                isError = labelError != null,
                supportingText = labelError?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth()
            )
            if (uri == null) {
                Text(text = stringResource(RString.add_item_slot_empty), style = MaterialTheme.typography.bodySmall)
                if (sources != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ROW_PADDING)) {
                        OutlinedButton(onClick = {
                            onSourceUse(slot.id)
                            sources.takePhoto()
                        }) { Text(stringResource(RString.add_item_take_photo)) }
                        OutlinedButton(onClick = {
                            onSourceUse(slot.id)
                            sources.pickPhotos()
                        }) { Text(stringResource(RString.add_item_pick_photo_one)) }
                    }
                }
            }
            Row {
                if (uri != null) {
                    IconButton(onClick = { uiState.onSlotRotate(slot.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(RString.add_item_rotate_photo)
                        )
                    }
                }
                IconButton(onClick = { uiState.onSlotRemove(slot.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(RString.add_item_remove_slot)
                    )
                }
            }
        }
    }
}

private val ROW_PADDING = 8.dp
