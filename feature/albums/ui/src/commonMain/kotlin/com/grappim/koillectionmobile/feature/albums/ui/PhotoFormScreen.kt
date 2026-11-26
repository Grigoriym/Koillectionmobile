package com.grappim.koillectionmobile.feature.albums.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_image_required
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.photo_form_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.photo_label_comment
import com.grappim.koillectionmobile.strings.generated.resources.photo_label_place
import com.grappim.koillectionmobile.strings.generated.resources.photo_label_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The new-photo form of album [albumId]. [onClose] runs once the photo is saved, to take the form off the back stack. */
@Composable
fun NewPhotoScreen(
    albumId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewPhotoViewModel = koinViewModel { parametersOf(albumId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    AlbumFormScaffold(
        topTitle = stringResource(RString.photo_form_new_title),
        hasInput = uiState.hasInput,
        canSave = uiState.canSave,
        isSaveShown = uiState.save == null,
        discardTitle = stringResource(RString.photo_form_discard_title),
        discardText = stringResource(RString.photo_form_discard_text),
        onSave = uiState.onSave,
        onBack = onBack,
        modifier = modifier
    ) { contentModifier ->
        val save = uiState.save
        if (save == null) {
            PhotoForm(
                values = uiState.values,
                onFieldChange = uiState.onFieldChange,
                photo = uiState.photo,
                imageUrl = null,
                isImageMissing = uiState.photo == null,
                onPhotosAdd = uiState.onPhotosAdd,
                onPhotoRotate = uiState.onPhotoRotate,
                onPhotoRemove = uiState.onPhotoRemove,
                modifier = contentModifier
            )
        } else {
            AlbumSaveView(
                status = save,
                partialText = stringResource(RString.photo_form_save_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )
        }
    }
}

/** The edit form of photo [photoId]. [onClose] runs once every change is on the server, to take the form off the back stack. */
@Composable
fun EditPhotoScreen(
    photoId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditPhotoViewModel = koinViewModel { parametersOf(photoId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    AlbumFormScaffold(
        topTitle = stringResource(RString.photo_form_edit_title),
        hasInput = uiState.changes.isNotEmpty(),
        canSave = uiState.canSave,
        isSaveShown = uiState.original != null && uiState.save == null,
        discardTitle = stringResource(RString.collection_form_edit_discard_title),
        discardText = stringResource(RString.photo_form_edit_discard_text),
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
                onRetry = uiState.onRetry
            )

            save != null -> AlbumSaveView(
                status = save,
                partialText = stringResource(RString.collection_form_edit_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )

            else -> PhotoForm(
                values = uiState.values,
                onFieldChange = uiState.onFieldChange,
                photo = uiState.photo,
                imageUrl = uiState.original?.imageUrl,
                isImageMissing = false,
                onPhotosAdd = uiState.onPhotosAdd,
                onPhotoRotate = uiState.onPhotoRotate,
                onPhotoRemove = uiState.onPhotoRemove,
                modifier = contentModifier
            )
        }
    }
}

/**
 * The text fields and the image. [photo] is a new photo on this device. Without one, [imageUrl] is
 * the image on the server (the edit form), with no remove button. [isImageMissing] shows that Save
 * needs an image (the new form).
 */
@Composable
private fun PhotoForm(
    values: PhotoFormValues,
    onFieldChange: (PhotoField, String) -> Unit,
    photo: FormPhoto?,
    imageUrl: String?,
    isImageMissing: Boolean,
    onPhotosAdd: (List<String>) -> Unit,
    onPhotoRotate: () -> Unit,
    onPhotoRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        PhotoTextField(values, PhotoField.TITLE, stringResource(RString.photo_label_title), onFieldChange)
        PhotoTextField(
            values = values,
            field = PhotoField.COMMENT,
            label = stringResource(RString.photo_label_comment),
            onFieldChange = onFieldChange,
            singleLine = false
        )
        PhotoTextField(values, PhotoField.PLACE, stringResource(RString.photo_label_place), onFieldChange)
        FormImageSection(
            photo = photo,
            imageUrl = imageUrl,
            isServerImageRemovable = false,
            onPhotosAdd = onPhotosAdd,
            onPhotoRotate = onPhotoRotate,
            onPhotoRemove = onPhotoRemove
        )
        if (isImageMissing) {
            Text(
                text = stringResource(RString.photo_form_image_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhotoTextField(
    values: PhotoFormValues,
    field: PhotoField,
    label: String,
    onFieldChange: (PhotoField, String) -> Unit,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = values.text(field),
        onValueChange = { onFieldChange(field, it) },
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}
