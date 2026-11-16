package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_discard
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_value
import com.grappim.koillectionmobile.strings.generated.resources.add_item_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.add_item_label_duplicate
import com.grappim.koillectionmobile.strings.generated.resources.add_item_label_required
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_category
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_tags
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_field
import com.grappim.koillectionmobile.strings.generated.resources.add_item_retry
import com.grappim.koillectionmobile.strings.generated.resources.add_item_section_tags
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/* The parts the add form and the edit form share. */

@Composable
internal fun FieldRowEditor(
    label: String,
    value: String,
    labelError: StringResource?,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(RString.add_item_field_label)) },
                singleLine = true,
                isError = labelError != null,
                supportingText = labelError?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = value,
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

@Composable
internal fun DiscardDialog(title: StringResource, text: StringResource, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(text)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(RString.add_item_discard)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(RString.add_item_keep_editing)) } }
    )
}

/** The message under a label field: a blank label, or one that another field uses. */
internal fun labelError(label: String, duplicates: Set<String>): StringResource? = when {
    label.isBlank() -> RString.add_item_label_required
    label in duplicates -> RString.add_item_label_duplicate
    else -> null
}

@Composable
internal fun TagPicker(
    tagGroups: List<TagGroup>,
    selectedTagIds: Set<String>,
    isLoading: Boolean,
    error: NativeText,
    onTagToggle: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
        Text(
            text = stringResource(RString.add_item_section_tags),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        when {
            isLoading -> CircularProgressIndicator()

            error.isNotEmpty() -> {
                Text(text = error.asString(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(RString.add_item_retry)) }
            }

            tagGroups.isEmpty() -> Text(stringResource(RString.add_item_no_tags))

            else -> tagGroups.forEach { group ->
                Text(
                    text = group.label ?: stringResource(RString.add_item_no_category),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
                    group.tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in selectedTagIds,
                            onClick = { onTagToggle(tag.id) },
                            label = { Text(tag.label) }
                        )
                    }
                }
            }
        }
    }
}

private val TAG_SPACING = 8.dp
