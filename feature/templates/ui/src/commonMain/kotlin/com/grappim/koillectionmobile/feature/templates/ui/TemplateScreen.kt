package com.grappim.koillectionmobile.feature.templates.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.template_delete
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.template_edit
import com.grappim.koillectionmobile.strings.generated.resources.template_empty
import com.grappim.koillectionmobile.strings.generated.resources.template_field_choice_list
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The fields of template [templateId]. [name] is the title until the template is loaded, as the
 * template list showed it. [onClose] runs once the template is deleted, to take the screen off the
 * back stack.
 */
@Composable
fun TemplateScreen(
    templateId: String,
    name: String,
    onEdit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplateViewModel = koinViewModel { parametersOf(templateId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    TemplateContent(uiState = uiState, name = name, onEdit = onEdit, modifier = modifier)
}

@Composable
private fun TemplateContent(uiState: TemplateUiState, name: String, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    var isDeleteAsked by rememberSaveable { mutableStateOf(false) }
    val title = uiState.name ?: name
    val topBarController = LocalTopBarConfig.current
    val editLabel = stringResource(RString.template_edit)
    val deleteLabel = stringResource(RString.template_delete)
    val currentOnEdit by rememberUpdatedState(onEdit)
    // The kit bar has no progress slot: while the delete runs, the actions stay, disabled, and the
    // progress shows under the bar.
    val hasActions = uiState.canChange || uiState.isDeleting
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
                            onClick = { currentOnEdit() }
                        ),
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = deleteLabel,
                            enabled = !uiState.isDeleting,
                            onClick = { isDeleteAsked = true }
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
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.fields.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.template_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.fields, key = { it.id }) { field ->
                    ListItem(
                        headlineContent = { Text(field.name) },
                        supportingContent = {
                            val type = fieldTypeLabel(field.type)?.let { stringResource(it) } ?: field.type
                            val choiceList = field.choiceListName
                            Text(
                                if (choiceList == null) {
                                    type
                                } else {
                                    stringResource(RString.template_field_choice_list, type, choiceList)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
    if (isDeleteAsked) {
        AlertDialog(
            onDismissRequest = { isDeleteAsked = false },
            title = { Text(stringResource(RString.template_delete_title)) },
            text = { Text(stringResource(RString.template_delete_text, title)) },
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
    if (uiState.deleteError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteErrorDismiss,
            title = { Text(stringResource(RString.template_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorDismiss) {
                    Text(stringResource(RString.template_delete_failed_ok))
                }
            }
        )
    }
}
