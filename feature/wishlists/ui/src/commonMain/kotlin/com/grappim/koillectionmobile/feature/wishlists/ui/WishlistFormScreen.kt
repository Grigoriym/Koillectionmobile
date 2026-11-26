package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.core.form.FormImageSection
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveView
import com.grappim.koillectionmobile.core.form.FormScaffold
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_name_label
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_save_partial
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The new-wishlist form, in [parentId] or at the top level when it is null. [onClose] runs once the
 * wishlist is saved, to take the form off the back stack.
 */
@Composable
fun NewWishlistScreen(
    parentId: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewWishlistViewModel = koinViewModel { parametersOf(parentId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    FormScaffold(
        topTitle = stringResource(RString.wishlist_form_new_title),
        hasInput = uiState.hasInput,
        canSave = uiState.canSave,
        isSaveShown = uiState.save == null,
        discardTitle = stringResource(RString.wishlist_form_discard_title),
        discardText = stringResource(RString.wishlist_form_discard_text),
        onSave = uiState.onSave,
        onBack = onBack,
        modifier = modifier
    ) { contentModifier ->
        val save = uiState.save
        if (save == null) {
            WishlistForm(
                name = uiState.name,
                onNameChange = uiState.onNameChange,
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
                partialText = stringResource(RString.wishlist_form_save_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )
        }
    }
}

/** The edit form of wishlist [wishlistId]. [onClose] runs once every change is on the server, to take the form off the back stack. */
@Composable
fun EditWishlistScreen(
    wishlistId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditWishlistViewModel = koinViewModel { parametersOf(wishlistId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    FormScaffold(
        topTitle = stringResource(RString.wishlist_form_edit_title),
        hasInput = uiState.changes.isNotEmpty(),
        canSave = uiState.canSave,
        isSaveShown = uiState.original != null && uiState.save == null,
        discardTitle = stringResource(RString.collection_form_edit_discard_title),
        discardText = stringResource(RString.wishlist_form_edit_discard_text),
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

            save != null -> FormSaveView(
                status = save,
                partialText = stringResource(RString.collection_form_edit_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )

            else -> WishlistForm(
                name = uiState.name,
                onNameChange = uiState.onNameChange,
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

/**
 * The name and the image. [photo] is a new photo on this device. Without one, [imageUrl] is the
 * image on the server, if the wishlist has one (the edit form).
 */
@Composable
private fun WishlistForm(
    name: String,
    onNameChange: (String) -> Unit,
    photo: FormPhoto?,
    imageUrl: String?,
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
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(RString.wishlist_form_name_label)) },
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
    }
}
