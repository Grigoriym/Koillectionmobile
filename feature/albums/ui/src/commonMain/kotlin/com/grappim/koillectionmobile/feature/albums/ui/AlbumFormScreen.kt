package com.grappim.koillectionmobile.feature.albums.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionTextButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.photos.PhotoPreview
import com.grappim.koillectionmobile.core.photos.rememberPhotoSources
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.album_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.album_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.album_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.album_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.album_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.album_form_save_partial
import com.grappim.koillectionmobile.strings.generated.resources.album_form_title_label
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_form
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_partial
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_pick_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_preparing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_remove_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_retry
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_section_image
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_sending
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_take_photo
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The new-album form, in [parentId] or at the top level when it is null. [onClose] runs once the
 * album is saved, to take the form off the back stack.
 */
@Composable
fun NewAlbumScreen(
    parentId: String?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewAlbumViewModel = koinViewModel { parametersOf(parentId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    AlbumFormScaffold(
        topTitle = stringResource(RString.album_form_new_title),
        hasInput = uiState.hasInput,
        canSave = uiState.canSave,
        isSaveShown = uiState.save == null,
        discardTitle = stringResource(RString.album_form_discard_title),
        discardText = stringResource(RString.album_form_discard_text),
        onSave = uiState.onSave,
        onBack = onBack,
        modifier = modifier
    ) { contentModifier ->
        val save = uiState.save
        if (save == null) {
            AlbumForm(
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
            AlbumSaveView(
                status = save,
                partialText = stringResource(RString.album_form_save_partial),
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )
        }
    }
}

/** The edit form of album [albumId]. [onClose] runs once every change is on the server, to take the form off the back stack. */
@Composable
fun EditAlbumScreen(
    albumId: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = koinViewModel { parametersOf(albumId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    AlbumFormScaffold(
        topTitle = stringResource(RString.album_form_edit_title),
        hasInput = uiState.changes.isNotEmpty(),
        canSave = uiState.canSave,
        isSaveShown = uiState.original != null && uiState.save == null,
        discardTitle = stringResource(RString.collection_form_edit_discard_title),
        discardText = stringResource(RString.album_form_edit_discard_text),
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

            else -> AlbumForm(
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

/**
 * The frame of the album and the photo forms, new and edit: the top bar with Save, and the discard confirm.
 * While [hasInput], one back tap must not lose the form: the arrow and the system back ask first.
 * [content] gets the modifier of the area under the top bar.
 */
@Composable
internal fun AlbumFormScaffold(
    topTitle: String,
    hasInput: Boolean,
    canSave: Boolean,
    isSaveShown: Boolean,
    discardTitle: String,
    discardText: String,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    var isDiscardShown by rememberSaveable { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = hasInput,
        onBackCompleted = { isDiscardShown = true }
    )
    if (isDiscardShown) {
        AlertDialog(
            onDismissRequest = { isDiscardShown = false },
            title = { Text(discardTitle) },
            text = { Text(discardText) },
            confirmButton = {
                TextButton(onClick = {
                    isDiscardShown = false
                    onBack()
                }) { Text(stringResource(RString.collection_form_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { isDiscardShown = false }) {
                    Text(stringResource(RString.collection_form_keep_editing))
                }
            }
        )
    }
    val topBarController = LocalTopBarConfig.current
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSave by rememberUpdatedState(onSave)
    LaunchedEffect(topTitle, hasInput, isSaveShown, canSave) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(topTitle),
                navigationIcon = NavigationIconConfig.Back(
                    onBackClick = { if (hasInput) isDiscardShown = true else currentOnBack() }
                ),
                actions = if (isSaveShown) {
                    persistentListOf(
                        TopBarActionTextButton(
                            text = NativeText.Resource(RString.collection_form_save),
                            enabled = canSave,
                            onClick = { currentOnSave() }
                        )
                    )
                } else {
                    persistentListOf()
                }
            )
        )
    }
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        content(
            Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

/**
 * The title and the image. [photo] is a new photo on this device. Without one, [imageUrl] is the
 * image on the server, if the album has one (the edit form).
 */
@Composable
private fun AlbumForm(
    title: String,
    onTitleChange: (String) -> Unit,
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
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(RString.album_form_title_label)) },
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

/**
 * The image part of the form, in its own column with [PADDING] spacing. [photo] is a new photo on
 * this device. Without one, [imageUrl] is the image on the server, with a remove button only when
 * [isServerImageRemovable] (a photo image cannot be removed, plan §5 decision 14).
 */
@Composable
internal fun FormImageSection(
    photo: FormPhoto?,
    imageUrl: String?,
    isServerImageRemovable: Boolean,
    onPhotosAdd: (List<String>) -> Unit,
    onPhotoRotate: () -> Unit,
    onPhotoRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoSources = rememberPhotoSources(onPhotos = onPhotosAdd)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PADDING)) {
        Text(
            text = stringResource(RString.collection_form_section_image),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (photo != null) {
            PhotoPreview(uri = photo.uri, rotation = photo.rotation, modifier = Modifier.size(PREVIEW_SIZE))
            Row(horizontalArrangement = Arrangement.spacedBy(PADDING / 2)) {
                OutlinedButton(onClick = onPhotoRotate) {
                    Text(stringResource(RString.collection_form_rotate_photo))
                }
                OutlinedButton(onClick = onPhotoRemove) {
                    Text(stringResource(RString.collection_form_remove_photo))
                }
            }
        } else if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(PREVIEW_SIZE),
                contentScale = ContentScale.Fit
            )
            if (isServerImageRemovable) {
                OutlinedButton(onClick = onPhotoRemove) {
                    Text(stringResource(RString.collection_form_remove_photo))
                }
            }
        }
        if (photoSources != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(PADDING / 2)) {
                OutlinedButton(onClick = photoSources.takePhoto) {
                    Text(stringResource(RString.collection_form_take_photo))
                }
                OutlinedButton(onClick = photoSources.pickPhotos) {
                    Text(stringResource(RString.collection_form_pick_photo))
                }
            }
        }
    }
}

/**
 * The save instead of the form: a progress bar while it runs; on a failure the message and a
 * retry. The way back to the form is offered only while no call went through; otherwise
 * [partialText] says what is on the server.
 */
@Composable
internal fun AlbumSaveView(
    status: AlbumSaveStatus,
    partialText: String,
    onRetry: () -> Unit,
    onEditForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (status) {
            AlbumSaveStatus.Preparing -> {
                CircularProgressIndicator()
                Text(stringResource(RString.collection_form_preparing))
            }

            is AlbumSaveStatus.Sending -> {
                LinearProgressIndicator(
                    progress = { status.done.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(RString.collection_form_sending, status.done, status.total))
            }

            is AlbumSaveStatus.Failed -> {
                Text(text = status.message.asString(), color = MaterialTheme.colorScheme.error)
                if (status.isPartial) Text(partialText)
                Button(onClick = onRetry) { Text(stringResource(RString.collection_form_retry)) }
                if (!status.isPartial) {
                    OutlinedButton(onClick = onEditForm) { Text(stringResource(RString.collection_form_edit_form)) }
                }
            }
        }
    }
}

private val PREVIEW_SIZE = 160.dp
