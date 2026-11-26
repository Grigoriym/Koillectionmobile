package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_currency_error
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.wish_form_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_comment
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_currency
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_price
import com.grappim.koillectionmobile.strings.generated.resources.wish_label_url
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_form_name_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The new-wish form of wishlist [wishlistId]. [onClose] runs once the wish is saved, to take the form off the back stack. */
@Composable
fun NewWishScreen(
    wishlistId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewWishViewModel = koinViewModel { parametersOf(wishlistId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    WishlistFormScaffold(
        topTitle = stringResource(RString.wish_form_new_title),
        hasInput = uiState.hasInput,
        canSave = uiState.canSave,
        isSaveShown = uiState.save == null,
        discardTitle = stringResource(RString.wish_form_discard_title),
        discardText = stringResource(RString.wish_form_discard_text),
        onSave = uiState.onSave,
        onBack = onBack,
        modifier = modifier
    ) { contentModifier ->
        val save = uiState.save
        if (save == null) {
            WishForm(
                values = uiState.values,
                onFieldChange = uiState.onFieldChange,
                photo = uiState.photo,
                imageUrl = null,
                onPhotosAdd = uiState.onPhotosAdd,
                onPhotoRotate = uiState.onPhotoRotate,
                onPhotoRemove = uiState.onPhotoRemove,
                modifier = contentModifier
            )
        } else {
            WishlistSaveView(
                status = save,
                partialText = stringResource(RString.wish_form_save_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )
        }
    }
}

/** The edit form of wish [wishId]. [onClose] runs once every change is on the server, to take the form off the back stack. */
@Composable
fun EditWishScreen(
    wishId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditWishViewModel = koinViewModel { parametersOf(wishId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    WishlistFormScaffold(
        topTitle = stringResource(RString.wish_form_edit_title),
        hasInput = uiState.changes.isNotEmpty(),
        canSave = uiState.canSave,
        isSaveShown = uiState.original != null && uiState.save == null,
        discardTitle = stringResource(RString.collection_form_edit_discard_title),
        discardText = stringResource(RString.wish_form_edit_discard_text),
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

            save != null -> WishlistSaveView(
                status = save,
                partialText = stringResource(RString.collection_form_edit_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )

            else -> WishForm(
                values = uiState.values,
                onFieldChange = uiState.onFieldChange,
                photo = uiState.photo,
                imageUrl = uiState.original?.imageUrl,
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
 * the image on the server, if the wish has one (the edit form). The server image has no remove
 * button.
 */
@Composable
private fun WishForm(
    values: WishFormValues,
    onFieldChange: (WishField, String) -> Unit,
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
        WishTextField(values, WishField.NAME, stringResource(RString.wishlist_form_name_label), onFieldChange)
        WishTextField(
            values = values,
            field = WishField.URL,
            label = stringResource(RString.wish_label_url),
            onFieldChange = onFieldChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        WishTextField(values, WishField.PRICE, stringResource(RString.wish_label_price), onFieldChange)
        val isCurrencyError = !values.isCurrencyValid
        WishTextField(
            values = values,
            field = WishField.CURRENCY,
            label = stringResource(RString.wish_label_currency),
            onFieldChange = onFieldChange,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            isError = isCurrencyError,
            supportingText = if (isCurrencyError) {
                { Text(stringResource(RString.wish_form_currency_error)) }
            } else {
                null
            }
        )
        WishTextField(
            values = values,
            field = WishField.COMMENT,
            label = stringResource(RString.wish_label_comment),
            onFieldChange = onFieldChange,
            singleLine = false
        )
        FormImageSection(
            photo = photo,
            imageUrl = imageUrl,
            isServerImageRemovable = false,
            onPhotosAdd = onPhotosAdd,
            onPhotoRotate = onPhotoRotate,
            onPhotoRemove = onPhotoRemove
        )
    }
}

@Composable
private fun WishTextField(
    values: WishFormValues,
    field: WishField,
    label: String,
    onFieldChange: (WishField, String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = values.text(field),
        onValueChange = { onFieldChange(field, it) },
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        isError = isError,
        supportingText = supportingText,
        modifier = Modifier.fillMaxWidth()
    )
}
