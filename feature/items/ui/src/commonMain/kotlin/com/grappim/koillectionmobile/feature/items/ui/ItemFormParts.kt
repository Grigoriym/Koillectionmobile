package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.feature.items.domain.TEXT_TYPES
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHECKBOX
import com.grappim.koillectionmobile.feature.items.domain.TYPE_CHOICE_LIST
import com.grappim.koillectionmobile.feature.items.domain.TYPE_NUMBER
import com.grappim.koillectionmobile.feature.items.domain.TYPE_PRICE
import com.grappim.koillectionmobile.feature.items.domain.TYPE_RATING
import com.grappim.koillectionmobile.feature.items.domain.TYPE_TEXT
import com.grappim.koillectionmobile.feature.items.domain.TYPE_TEXTAREA
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.add_item_discard
import com.grappim.koillectionmobile.strings.generated.resources.add_item_edit_form
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_choice_list
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_choice_list_none
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_type
import com.grappim.koillectionmobile.strings.generated.resources.add_item_field_value
import com.grappim.koillectionmobile.strings.generated.resources.add_item_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.add_item_label_duplicate
import com.grappim.koillectionmobile.strings.generated.resources.add_item_label_required
import com.grappim.koillectionmobile.strings.generated.resources.add_item_new_tag
import com.grappim.koillectionmobile.strings.generated.resources.add_item_new_tag_add
import com.grappim.koillectionmobile.strings.generated.resources.add_item_new_tag_cancel
import com.grappim.koillectionmobile.strings.generated.resources.add_item_new_tag_label
import com.grappim.koillectionmobile.strings.generated.resources.add_item_new_tag_too_long
import com.grappim.koillectionmobile.strings.generated.resources.add_item_new_tags
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_category
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_choice_lists
import com.grappim.koillectionmobile.strings.generated.resources.add_item_no_tags
import com.grappim.koillectionmobile.strings.generated.resources.add_item_preparing
import com.grappim.koillectionmobile.strings.generated.resources.add_item_remove_field
import com.grappim.koillectionmobile.strings.generated.resources.add_item_retry
import com.grappim.koillectionmobile.strings.generated.resources.add_item_section_tags
import com.grappim.koillectionmobile.strings.generated.resources.add_item_sending
import com.grappim.koillectionmobile.strings.generated.resources.add_item_value_number
import com.grappim.koillectionmobile.strings.generated.resources.add_item_value_price
import com.grappim.koillectionmobile.strings.generated.resources.add_item_value_rating
import com.grappim.koillectionmobile.strings.generated.resources.field_type_checkbox
import com.grappim.koillectionmobile.strings.generated.resources.field_type_choice_list
import com.grappim.koillectionmobile.strings.generated.resources.field_type_country
import com.grappim.koillectionmobile.strings.generated.resources.field_type_date
import com.grappim.koillectionmobile.strings.generated.resources.field_type_link
import com.grappim.koillectionmobile.strings.generated.resources.field_type_list
import com.grappim.koillectionmobile.strings.generated.resources.field_type_number
import com.grappim.koillectionmobile.strings.generated.resources.field_type_price
import com.grappim.koillectionmobile.strings.generated.resources.field_type_rating
import com.grappim.koillectionmobile.strings.generated.resources.field_type_text
import com.grappim.koillectionmobile.strings.generated.resources.field_type_textarea
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/* The parts the add form and the edit form share. */

/**
 * One label/value row: the label, the type (a picker when [canChangeType], else shown for a type
 * other than `text`), and the input of the type (plan §5 decision 11).
 */
@Composable
internal fun FieldRowEditor(
    label: String,
    value: String,
    type: String,
    choiceListId: String?,
    labelError: StringResource?,
    canChangeType: Boolean,
    choiceLists: ChoiceListsUi,
    actions: FieldRowActions,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = label,
                onValueChange = actions.onLabelChange,
                label = { Text(stringResource(RString.add_item_field_label)) },
                singleLine = true,
                isError = labelError != null,
                supportingText = labelError?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth()
            )
            val typeName = TYPE_LABELS[type]?.let { stringResource(it) } ?: type
            if (canChangeType) {
                OptionPicker(
                    label = stringResource(RString.add_item_field_type),
                    selected = typeName,
                    options = TEXT_TYPES.map { it to (TYPE_LABELS[it]?.let { name -> stringResource(name) } ?: it) },
                    onSelect = actions.onTypeChange
                )
            } else if (type != TYPE_TEXT) {
                Text(
                    text = "${stringResource(RString.add_item_field_type)}: $typeName",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            when (type) {
                TYPE_CHECKBOX -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(RString.add_item_field_value), modifier = Modifier.weight(1f))
                    Switch(checked = isChecked(value), onCheckedChange = actions.onCheckedChange)
                }

                TYPE_CHOICE_LIST -> ChoiceListInput(
                    value = value,
                    choiceListId = choiceListId,
                    canChangeList = canChangeType,
                    choiceLists = choiceLists,
                    actions = actions
                )

                else -> TextValueInput(value = value, type = type, onValueChange = actions.onValueChange)
            }
        }
        IconButton(onClick = actions.onRemove) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(RString.add_item_remove_field))
        }
    }
}

/** A text box. A `number`, `price` or `rating` gets a number keyboard and the value check of [isValueInvalid]. */
@Composable
private fun TextValueInput(value: String, type: String, onValueChange: (String) -> Unit) {
    val hint = VALUE_HINTS[type]
    val isInvalid = isValueInvalid(type, value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(RString.add_item_field_value)) },
        singleLine = hint != null,
        keyboardOptions = if (hint !=
            null
        ) {
            KeyboardOptions(keyboardType = KeyboardType.Decimal)
        } else {
            KeyboardOptions.Default
        },
        isError = isInvalid,
        supportingText = if (isInvalid && hint != null) {
            { Text(stringResource(hint)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * The choices of the row's choice list, each one a chip to pick or unpick. A new row picks its
 * list first ([canChangeList]). A pick that is no longer in the list still shows, so it can be
 * unpicked.
 */
@Composable
private fun ChoiceListInput(
    value: String,
    choiceListId: String?,
    canChangeList: Boolean,
    choiceLists: ChoiceListsUi,
    actions: FieldRowActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val list = choiceLists.lists.firstOrNull { it.id == choiceListId }
        when {
            choiceLists.isLoading -> CircularProgressIndicator()

            choiceLists.error.isNotEmpty() -> {
                Text(text = choiceLists.error.asString(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = actions.onRetryChoiceLists) { Text(stringResource(RString.add_item_retry)) }
            }

            !canChangeList -> Unit

            choiceLists.lists.isEmpty() ->
                Text(stringResource(RString.add_item_no_choice_lists), color = MaterialTheme.colorScheme.error)

            else -> OptionPicker(
                label = stringResource(RString.add_item_field_choice_list),
                selected = list?.name ?: stringResource(RString.add_item_field_choice_list_none),
                options = choiceLists.lists.map { it.id to it.name },
                onSelect = actions.onChoiceListChange
            )
        }
        val picked = pickedChoices(value)
        val choices = list?.choices.orEmpty()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
            (choices + picked.filterNot { it in choices }).forEach { choice ->
                FilterChip(
                    selected = choice in picked,
                    onClick = { actions.onChoiceToggle(choice) },
                    label = { Text(choice) }
                )
            }
        }
    }
}

/** A button that shows [label] and [selected], and opens a menu of [options] (value to text). */
@Composable
private fun OptionPicker(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { isExpanded = true }) { Text(selected) }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            isExpanded = false
                            onSelect(value)
                        }
                    )
                }
            }
        }
    }
}

/** A readable name of each of `TEXT_TYPES`, the same as the template form's. */
private val TYPE_LABELS: Map<String, StringResource> = mapOf(
    TYPE_TEXT to RString.field_type_text,
    TYPE_TEXTAREA to RString.field_type_textarea,
    "country" to RString.field_type_country,
    "date" to RString.field_type_date,
    TYPE_RATING to RString.field_type_rating,
    TYPE_NUMBER to RString.field_type_number,
    TYPE_PRICE to RString.field_type_price,
    "link" to RString.field_type_link,
    "list" to RString.field_type_list,
    TYPE_CHOICE_LIST to RString.field_type_choice_list,
    TYPE_CHECKBOX to RString.field_type_checkbox
)

/** The types with a number keyboard, and the message for a value the server rejects. */
private val VALUE_HINTS: Map<String, StringResource> = mapOf(
    TYPE_NUMBER to RString.add_item_value_number,
    TYPE_PRICE to RString.add_item_value_price,
    TYPE_RATING to RString.add_item_value_rating
)

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
    newTagLabels: List<String>,
    isLoading: Boolean,
    error: NativeText,
    onTagToggle: (String) -> Unit,
    onNewTag: (String) -> Unit,
    onNewTagRemove: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isNewTagShown by rememberSaveable { mutableStateOf(false) }
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
        if (newTagLabels.isNotEmpty()) {
            Text(text = stringResource(RString.add_item_new_tags), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TAG_SPACING)) {
                newTagLabels.forEach { label ->
                    FilterChip(selected = true, onClick = { onNewTagRemove(label) }, label = { Text(label) })
                }
            }
        }
        if (!isLoading) {
            TextButton(onClick = { isNewTagShown = true }) { Text(stringResource(RString.add_item_new_tag)) }
        }
    }
    if (isNewTagShown) {
        NewTagDialog(
            onAdd = {
                onNewTag(it)
                isNewTagShown = false
            },
            onDismiss = { isNewTagShown = false }
        )
    }
}

/** Asks for the label of a new tag. The server stores at most [MAX_TAG_LABEL] characters. */
@Composable
private fun NewTagDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    val isTooLong = label.trim().length > MAX_TAG_LABEL
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.add_item_new_tag)) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(RString.add_item_new_tag_label)) },
                singleLine = true,
                isError = isTooLong,
                supportingText = if (isTooLong) {
                    { Text(stringResource(RString.add_item_new_tag_too_long, MAX_TAG_LABEL)) }
                } else {
                    null
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(label) }, enabled = label.isNotBlank() && !isTooLong) {
                Text(stringResource(RString.add_item_new_tag_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.add_item_new_tag_cancel)) }
        }
    )
}

/** `Tag.label` is a Doctrine `string` column: 255 characters, and the entity has no length check. */
private const val MAX_TAG_LABEL = 255

/**
 * The save instead of the form: a progress bar while it runs; on a failure the message and a
 * retry. [partialText] explains a partial save (some calls went through); only then is the way
 * back to the form ([onEditForm]) not offered.
 */
@Composable
internal fun SaveView(
    status: SaveStatus,
    partialText: StringResource,
    onRetry: () -> Unit,
    onEditForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(SAVE_PADDING),
        verticalArrangement = Arrangement.spacedBy(SAVE_PADDING, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            SaveStatus.Preparing -> {
                CircularProgressIndicator()
                Text(stringResource(RString.add_item_preparing))
            }

            is SaveStatus.Sending -> {
                LinearProgressIndicator(
                    progress = { if (status.total == 0) 0f else status.done.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(RString.add_item_sending, status.done, status.total))
            }

            is SaveStatus.Failed -> {
                Text(text = status.message.asString(), color = MaterialTheme.colorScheme.error)
                if (status.isPartial) Text(stringResource(partialText))
                Button(onClick = onRetry) { Text(stringResource(RString.add_item_retry)) }
                if (!status.isPartial) {
                    OutlinedButton(onClick = onEditForm) { Text(stringResource(RString.add_item_edit_form)) }
                }
            }
        }
    }
}

private val TAG_SPACING = 8.dp
private val SAVE_PADDING = 16.dp
