package com.grappim.koillectionmobile.feature.templates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_back
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_form
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_retry
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_sending
import com.grappim.koillectionmobile.strings.generated.resources.template_form_add_field
import com.grappim.koillectionmobile.strings.generated.resources.template_form_choice_lists
import com.grappim.koillectionmobile.strings.generated.resources.template_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.template_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.template_form_duplicate_name
import com.grappim.koillectionmobile.strings.generated.resources.template_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.template_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.template_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.template_form_field_choice_list_label
import com.grappim.koillectionmobile.strings.generated.resources.template_form_field_choice_list_none
import com.grappim.koillectionmobile.strings.generated.resources.template_form_field_name_label
import com.grappim.koillectionmobile.strings.generated.resources.template_form_field_type_label
import com.grappim.koillectionmobile.strings.generated.resources.template_form_move_down
import com.grappim.koillectionmobile.strings.generated.resources.template_form_move_up
import com.grappim.koillectionmobile.strings.generated.resources.template_form_name_label
import com.grappim.koillectionmobile.strings.generated.resources.template_form_new_partial
import com.grappim.koillectionmobile.strings.generated.resources.template_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.template_form_no_choice_lists
import com.grappim.koillectionmobile.strings.generated.resources.template_form_remove_field
import com.grappim.koillectionmobile.strings.generated.resources.template_form_section_fields
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The template form: a new template when [templateId] is null, else the edit form. [onSave] runs
 * once every change is on the server, with the template's id, to take the form off the back stack.
 * [onChoiceListsClick] opens the choice lists from a `choice-list` field.
 */
@Composable
fun TemplateFormScreen(
    templateId: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onChoiceListsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplateFormViewModel = koinViewModel { parametersOf(templateId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSave by rememberUpdatedState(onSave)
    LaunchedEffect(uiState.savedTemplateId) {
        uiState.savedTemplateId?.let { currentOnSave(it) }
    }
    TemplateFormContent(
        uiState = uiState,
        onBack = onBack,
        onChoiceListsClick = onChoiceListsClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateFormContent(
    uiState: TemplateFormUiState,
    onBack: () -> Unit,
    onChoiceListsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // While the form has changes, one back tap must not lose them: the arrow and the system back ask first.
    var isDiscardShown by rememberSaveable { mutableStateOf(false) }
    val asksFirst = uiState.hasChanges && uiState.save == null
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = asksFirst,
        onBackCompleted = { isDiscardShown = true }
    )
    if (isDiscardShown) {
        DiscardDialog(isNew = uiState.isNew, onDiscard = {
            isDiscardShown = false
            onBack()
        }, onDismiss = { isDiscardShown = false })
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNew) RString.template_form_new_title else RString.template_form_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (asksFirst) isDiscardShown = true else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(RString.collection_form_back)
                        )
                    }
                },
                actions = {
                    if (uiState.draft != null && uiState.save == null) {
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
        val draft = uiState.draft
        val save = uiState.save
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || draft == null -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = "",
                onRetry = uiState.onRetry
            )

            save != null -> SaveView(
                status = save,
                isNew = uiState.isNew,
                uiState = uiState,
                modifier = contentModifier
            )

            else -> TemplateForm(
                draft = draft,
                uiState = uiState,
                onChoiceListsClick = onChoiceListsClick,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun DiscardDialog(isNew: Boolean, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNew) RString.template_form_discard_title else RString.template_form_edit_discard_title
                )
            )
        },
        text = {
            Text(
                stringResource(
                    if (isNew) RString.template_form_discard_text else RString.template_form_edit_discard_text
                )
            )
        },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(RString.collection_form_discard)) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.collection_form_keep_editing)) }
        }
    )
}

@Composable
private fun TemplateForm(
    draft: TemplateDraft,
    uiState: TemplateFormUiState,
    onChoiceListsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duplicates = uiState.duplicateNames
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = uiState.onNameChange,
                label = { Text(stringResource(RString.template_form_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                text = stringResource(RString.template_form_section_fields),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(draft.fields, key = { it.id }) { field ->
            FieldCard(
                field = field,
                isFirst = field.id == draft.fields.first().id,
                isLast = field.id == draft.fields.last().id,
                isDuplicate = field.name.trim() in duplicates,
                choiceLists = uiState.choiceLists,
                uiState = uiState,
                onChoiceListsClick = onChoiceListsClick
            )
        }
        item {
            OutlinedButton(onClick = uiState.onAddField) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(RString.template_form_add_field))
            }
        }
    }
}

@Composable
private fun FieldCard(
    field: FormField,
    isFirst: Boolean,
    isLast: Boolean,
    isDuplicate: Boolean,
    choiceLists: List<ChoiceList>,
    uiState: TemplateFormUiState,
    onChoiceListsClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(PADDING), verticalArrangement = Arrangement.spacedBy(PADDING / 2)) {
            OutlinedTextField(
                value = field.name,
                onValueChange = { uiState.onFieldNameChange(field.id, it) },
                label = { Text(stringResource(RString.template_form_field_name_label)) },
                isError = isDuplicate,
                supportingText = if (isDuplicate) {
                    { Text(stringResource(RString.template_form_duplicate_name)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Picker(
                label = stringResource(RString.template_form_field_type_label),
                selected = fieldTypeLabel(field.type)?.let { stringResource(it) } ?: field.type,
                options = FIELD_TYPES.map { (type, label) -> type to stringResource(label) },
                onSelect = { uiState.onFieldTypeChange(field.id, it) }
            )
            if (field.type == TYPE_CHOICE_LIST) {
                if (choiceLists.isEmpty()) {
                    Text(stringResource(RString.template_form_no_choice_lists), color = MaterialTheme.colorScheme.error)
                } else {
                    Picker(
                        label = stringResource(RString.template_form_field_choice_list_label),
                        selected = choiceLists.firstOrNull { it.id == field.choiceListId }?.name
                            ?: stringResource(RString.template_form_field_choice_list_none),
                        options = choiceLists.map { it.id to it.name },
                        onSelect = { uiState.onFieldChoiceListChange(field.id, it) }
                    )
                }
                TextButton(onClick = onChoiceListsClick) {
                    Text(stringResource(RString.template_form_choice_lists))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { uiState.onMoveUp(field.id) }, enabled = !isFirst) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(RString.template_form_move_up)
                    )
                }
                IconButton(onClick = { uiState.onMoveDown(field.id) }, enabled = !isLast) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(RString.template_form_move_down)
                    )
                }
                IconButton(onClick = { uiState.onRemoveField(field.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(RString.template_form_remove_field))
                }
            }
        }
    }
}

/** A button that shows [label] and [selected], and opens a menu of [options] (value to text). */
@Composable
private fun Picker(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
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

/**
 * The save instead of the form: a progress bar while it runs; on a failure the message and a
 * retry. The way back to the form is offered only while no call went through.
 */
@Composable
private fun SaveView(
    status: TemplateSaveStatus,
    isNew: Boolean,
    uiState: TemplateFormUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            is TemplateSaveStatus.Sending -> {
                if (status.total == 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { status.done.toFloat() / status.total },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(RString.collection_form_sending, status.done, status.total))
                }
            }

            is TemplateSaveStatus.Failed -> {
                Text(text = status.message.asString(), color = MaterialTheme.colorScheme.error)
                if (status.isPartial) {
                    Text(
                        stringResource(
                            if (isNew) RString.template_form_new_partial else RString.collection_form_edit_partial
                        )
                    )
                }
                Button(onClick = uiState.onRetrySave) { Text(stringResource(RString.collection_form_retry)) }
                if (!status.isPartial) {
                    OutlinedButton(onClick = uiState.onEditForm) {
                        Text(stringResource(RString.collection_form_edit_form))
                    }
                }
            }
        }
    }
}
