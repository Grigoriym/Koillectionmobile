package com.grappim.koillectionmobile.feature.collections.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.core.form.FormImageSection
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveView
import com.grappim.koillectionmobile.core.form.FormScaffold
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_title_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The new-collection form, in [parentId] or at the top level when it is null. [onOpenCollection]
 * runs once the collection is saved, with its id.
 */
@Composable
fun NewCollectionScreen(
    parentId: String?,
    onBack: () -> Unit,
    onOpenCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewCollectionViewModel = koinViewModel { parametersOf(parentId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpen by rememberUpdatedState(onOpenCollection)
    LaunchedEffect(uiState.savedCollectionId) {
        uiState.savedCollectionId?.let { currentOnOpen(it) }
    }
    NewCollectionContent(uiState = uiState, onBack = onBack, modifier = modifier)
}

@Composable
private fun NewCollectionContent(uiState: NewCollectionUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    FormScaffold(
        topTitle = stringResource(RString.collection_form_new_title),
        hasInput = uiState.hasInput,
        canSave = uiState.canSave,
        isSaveShown = uiState.save == null,
        discardTitle = stringResource(RString.collection_form_discard_title),
        discardText = stringResource(RString.collection_form_discard_text),
        onSave = uiState.onSave,
        onBack = onBack,
        modifier = modifier
    ) { contentModifier ->
        val save = uiState.save
        if (save == null) {
            CollectionForm(
                title = uiState.title,
                onTitleChange = uiState.onTitleChange,
                photo = uiState.photo,
                imageUrl = null,
                onPhotosAdd = uiState.onPhotosAdd,
                onPhotoRotate = uiState.onPhotoRotate,
                onPhotoRemove = uiState.onPhotoRemove,
                modifier = contentModifier
            )
        } else {
            FormSaveView(
                status = save,
                partialText = stringResource(RString.collection_form_save_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )
        }
    }
}

/**
 * The title and the image. [photo] is a new photo on this device. Without one, [imageUrl] is the
 * image on the server, if the collection has one (the edit form). [extra] goes after the image: the
 * edit form puts its default-template picker there.
 */
@Composable
internal fun CollectionForm(
    title: String,
    onTitleChange: (String) -> Unit,
    photo: FormPhoto?,
    imageUrl: String?,
    onPhotosAdd: (List<String>) -> Unit,
    onPhotoRotate: () -> Unit,
    onPhotoRemove: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(RString.collection_form_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        FormImageSection(
            photo = photo,
            imageUrl = imageUrl,
            isServerImageRemovable = true,
            onPhotosAdd = onPhotosAdd,
            onPhotoRotate = onPhotoRotate,
            onPhotoRemove = onPhotoRemove
        )
        extra()
    }
}
