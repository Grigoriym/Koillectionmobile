package com.grappim.koillectionmobile.feature.items.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.grappim.kit.logger.logcat
import com.grappim.koillectionmobile.core.image.ImagePreparer
import com.grappim.koillectionmobile.core.image.Rotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The camera writes to a file in the cache (`photo_paths.xml`), shared through the `FileProvider`
 * in this module's manifest. The camera URI waits in saved state: the camera app often pushes this
 * process out of memory before it returns.
 */
@Composable
internal actual fun rememberPhotoSources(onPhotos: (List<String>) -> Unit): PhotoSources? {
    val context = LocalContext.current
    val currentOnPhotos by rememberUpdatedState(onPhotos)
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri ?: return@rememberLauncherForActivityResult
        pendingCameraUri = null
        if (saved) {
            currentOnPhotos(listOf(uri))
        } else {
            context.contentResolver.delete(Uri.parse(uri), null, null)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { context.keepReadAccess(it) }
        if (uris.isNotEmpty()) currentOnPhotos(uris.map(Uri::toString))
    }

    return remember(camera, picker) {
        PhotoSources(
            takePhoto = {
                val uri = context.newCameraUri()
                pendingCameraUri = uri.toString()
                camera.launch(uri)
            },
            pickPhotos = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

/**
 * Runs M3.2's `ImagePreparer` at [PREVIEW_LONG_SIDE_PX], so the preview shows the upload's
 * orientation. A photo that cannot be read shows nothing.
 */
@Composable
internal actual fun PhotoPreview(uri: String, rotation: Rotation, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri, rotation) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImagePreparer(context.contentResolver)
                    .bitmap(Uri.parse(uri), rotation, PREVIEW_LONG_SIDE_PX)
                    .asImageBitmap()
            }.onFailure { logcat(tag = TAG) { "Preview of $uri failed: $it" } }
                .getOrNull()
        }
    }
    Box(modifier = modifier) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun Context.newCameraUri(): Uri {
    val dir = File(cacheDir, CAMERA_DIR).apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName$AUTHORITY_SUFFIX", file)
}

/**
 * A picker URI is readable only until the process ends. A persisted grant keeps it readable after
 * process death. The picker's fallback on old devices (`ACTION_GET_CONTENT`) may not offer one:
 * then the photo is readable only in this process.
 */
private fun Context.keepReadAccess(uri: Uri) {
    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        .onFailure { logcat(tag = TAG) { "No persistable read grant for $uri: $it" } }
}

private const val PREVIEW_LONG_SIDE_PX = 512
private const val CAMERA_DIR = "camera"

/** Must match the provider authority in this module's `AndroidManifest.xml`. */
private const val AUTHORITY_SUFFIX = ".photos"
private const val TAG = "PhotoSources"
