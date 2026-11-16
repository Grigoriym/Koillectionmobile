package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_title
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
            )
        }
    }
}
