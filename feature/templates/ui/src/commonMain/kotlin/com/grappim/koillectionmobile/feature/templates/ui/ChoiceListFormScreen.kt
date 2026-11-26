package com.grappim.koillectionmobile.feature.templates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionTextButton
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_delete
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_add_choice
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_choice_label
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_duplicate_choice
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_name_label
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_remove_choice
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_section_choices
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.template_form_move_down
import com.grappim.koillectionmobile.strings.generated.resources.template_form_move_up
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The choice list form: a new choice list when [choiceListId] is null, else the edit form, which
 * also deletes the list. [onDone] runs once the save or the delete went through, to take the form
 * off the back stack.
 */
@Composable
fun ChoiceListFormScreen(
    choiceListId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChoiceListFormViewModel = koinViewModel { parametersOf(choiceListId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    var isDeleteAsked by rememberSaveable { mutableStateOf(false) }
    val topBarController = LocalTopBarConfig.current
    val deleteLabel = stringResource(RString.choice_list_delete)
    val currentOnSave by rememberUpdatedState(uiState.onSave)
    val hasDraft = uiState.draft != null
    // The kit bar has no progress slot: while a call runs, the actions stay, disabled, and the
    // progress shows under the bar.
    LaunchedEffect(uiState.isNew, hasDraft, uiState.canDelete, uiState.canSave, uiState.isBusy, deleteLabel) {
        val isBusy = uiState.isBusy
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(
                    if (uiState.isNew) RString.choice_list_form_new_title else RString.choice_list_form_edit_title
                ),
                navigationIcon = NavigationIconConfig.Back(),
                actions = if (hasDraft) {
                    listOfNotNull(
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = deleteLabel,
                            enabled = !isBusy,
                            onClick = { isDeleteAsked = true }
                        ).takeIf { uiState.canDelete },
                        TopBarActionTextButton(
                            text = NativeText.Resource(RString.collection_form_save),
                            enabled = uiState.canSave && !isBusy,
                            onClick = { currentOnSave() }
                        )
                    ).toPersistentList()
                } else {
                    persistentListOf()
                }
            )
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        val draft = uiState.draft
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

            else -> ChoiceListForm(draft = draft, uiState = uiState, modifier = contentModifier)
        }
    }
    if (isDeleteAsked) {
        AlertDialog(
            onDismissRequest = { isDeleteAsked = false },
            title = { Text(stringResource(RString.choice_list_delete_title)) },
            text = { Text(stringResource(RString.choice_list_delete_text, uiState.original?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    isDeleteAsked = false
                    uiState.onDelete()
                }) { Text(stringResource(RString.template_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteAsked = false }) { Text(stringResource(RString.template_delete_cancel)) }
            }
        )
    }
    if (uiState.actionError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onActionErrorDismiss,
            title = { Text(stringResource(RString.choice_list_form_failed_title)) },
            text = { Text(uiState.actionError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onActionErrorDismiss) {
                    Text(stringResource(RString.template_delete_failed_ok))
                }
            }
        )
    }
}

@Composable
private fun ChoiceListForm(draft: ChoiceListDraft, uiState: ChoiceListFormUiState, modifier: Modifier = Modifier) {
    val duplicates = uiState.duplicateChoices
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING / 2)
    ) {
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = uiState.onNameChange,
                label = { Text(stringResource(RString.choice_list_form_name_label)) },
                singleLine = true,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                text = stringResource(RString.choice_list_form_section_choices),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        itemsIndexed(draft.choices, key = { _, choice -> choice.id }) { index, choice ->
            val isDuplicate = choice.value.trim() in duplicates
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = choice.value,
                    onValueChange = { uiState.onChoiceChange(choice.id, it) },
                    label = { Text(stringResource(RString.choice_list_form_choice_label)) },
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text(stringResource(RString.choice_list_form_duplicate_choice)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { uiState.onMoveUp(choice.id) }, enabled = index > 0 && !uiState.isBusy) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(RString.template_form_move_up)
                    )
                }
                IconButton(
                    onClick = { uiState.onMoveDown(choice.id) },
                    enabled = index < draft.choices.lastIndex && !uiState.isBusy
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(RString.template_form_move_down)
                    )
                }
                IconButton(onClick = { uiState.onRemoveChoice(choice.id) }, enabled = !uiState.isBusy) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(RString.choice_list_form_remove_choice)
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = uiState.onAddChoice, enabled = !uiState.isBusy) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(RString.choice_list_form_add_choice))
            }
        }
    }
}
