package com.grappim.koillectionmobile.core.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.grappim.koillectionmobile.core.photos.PhotoPreview
import com.grappim.koillectionmobile.core.photos.rememberPhotoSources
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_pick_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_remove_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_rotate_photo
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_section_image
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_take_photo
import org.jetbrains.compose.resources.stringResource

/**
 * The image part of the form, in its own column with [PADDING] spacing. [photo] is a new photo on
 * this device. Without one, [imageUrl] is the image on the server, with a remove button only when
 * [isServerImageRemovable] (a wish or photo image cannot be removed, plan §5 decisions 12 and 14).
 */
@Composable
fun FormImageSection(
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

private val PREVIEW_SIZE = 160.dp
private val PADDING = 16.dp
