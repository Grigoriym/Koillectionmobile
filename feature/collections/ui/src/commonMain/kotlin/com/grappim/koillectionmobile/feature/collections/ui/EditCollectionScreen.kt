package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_default_template_hint
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_default_template_none
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_retry
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_section_default_template
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_templates_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The edit form of collection [collectionId]. [onClose] runs once every change is on the server, to take the form off the back stack. */
@Composable
fun EditCollectionScreen(
    collectionId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditCollectionViewModel = koinViewModel { parametersOf(collectionId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    EditCollectionContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@Composable
private fun EditCollectionContent(uiState: EditCollectionUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    CollectionFormScaffold(
        topTitle = stringResource(RString.collection_form_edit_title),
        hasInput = uiState.changes.isNotEmpty(),
        canSave = uiState.canSave,
        isSaveShown = uiState.original != null && uiState.save == null,
        discardTitle = stringResource(RString.collection_form_edit_discard_title),
        discardText = stringResource(RString.collection_form_edit_discard_text),
        onSave = uiState.onSave,
        onBack = onBack,
        modifier = modifier
    ) { contentModifier ->
        val save = uiState.save
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.original == null -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = "",
                onRetry = uiState.onRetry,
                retryEnabled = true
            )

            save != null -> CollectionSaveView(
                status = save,
                partialText = stringResource(RString.collection_form_edit_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )

            else -> CollectionForm(
                title = uiState.title,
                onTitleChange = uiState.onTitleChange,
                photo = uiState.photo,
                imageUrl = uiState.imageUrl,
                onPhotosAdd = uiState.onPhotosAdd,
                onPhotoRotate = uiState.onPhotoRotate,
                onPhotoRemove = uiState.onPhotoRemove,
                modifier = contentModifier
            ) {
                DefaultTemplatePicker(
                    selectedId = uiState.defaultTemplateId,
                    templates = uiState.templates,
                    onSelect = uiState.onDefaultTemplateChange,
                    onRetry = uiState.onRetryTemplates
                )
            }
        }
    }
}

/**
 * The default template: a button that shows the pick and opens a menu of "None" and the
 * [templates]. A pick that is not in the list (the list failed or is loading) shows its id.
 */
@Composable
private fun DefaultTemplatePicker(
    selectedId: String?,
    templates: TemplatesUi,
    onSelect: (String?) -> Unit,
    onRetry: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val none = stringResource(RString.collection_form_default_template_none)
    Column(verticalArrangement = Arrangement.spacedBy(PICKER_SPACING)) {
        Text(
            text = stringResource(RString.collection_form_section_default_template),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(RString.collection_form_default_template_hint),
            style = MaterialTheme.typography.bodySmall
        )
        Box {
            OutlinedButton(onClick = { isExpanded = true }) {
                Text(selectedId?.let { id -> templates.templates.firstOrNull { it.id == id }?.name ?: id } ?: none)
            }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(none) },
                    onClick = {
                        isExpanded = false
                        onSelect(null)
                    }
                )
                templates.templates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.name) },
                        onClick = {
                            isExpanded = false
                            onSelect(template.id)
                        }
                    )
                }
            }
        }
        when {
            templates.isLoading -> Text(
                text = stringResource(RString.collection_form_templates_loading),
                style = MaterialTheme.typography.bodySmall
            )

            templates.error != NativeText.Empty -> Row(
                horizontalArrangement = Arrangement.spacedBy(PICKER_SPACING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = templates.error.asString(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(RString.collection_form_retry)) }
            }
        }
    }
}

private val PICKER_SPACING = 8.dp
