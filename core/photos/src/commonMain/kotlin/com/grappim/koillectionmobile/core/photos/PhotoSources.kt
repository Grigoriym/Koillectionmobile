package com.grappim.koillectionmobile.core.photos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grappim.koillectionmobile.core.image.Rotation

/** Starts the system camera ([takePhoto]) or the photo picker ([pickPhotos]). */
class PhotoSources(val takePhoto: () -> Unit, val pickPhotos: () -> Unit)

/**
 * The photo sources of the platform. Each new photo's content URI goes to [onPhotos], in order.
 * Null where the app has no photo source (JVM and iOS, which are not shipped).
 */
@Composable
expect fun rememberPhotoSources(onPhotos: (List<String>) -> Unit): PhotoSources?

/** The photo at [uri] as the upload will be: EXIF orientation, then [rotation]. */
@Composable
expect fun PhotoPreview(uri: String, rotation: Rotation, modifier: Modifier = Modifier)
