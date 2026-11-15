package com.grappim.koillectionmobile.feature.items.ui

import android.content.Context
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Every photo ends up as a file in the cache (`photo_paths.xml`), shared through the `FileProvider`
 * in this module's manifest. The camera writes there itself. A picked photo is copied there: the
 * picker's URI is readable only until the process ends, and a copy needs no persisted grant
 * (Android keeps at most 512 per app). The camera URI waits in saved state: the camera app often
 * pushes this process out of memory before it returns.
 */
@Composable
internal actual fun rememberPhotoSources(onPhotos: (List<String>) -> Unit): PhotoSources? {
    val context = LocalContext.current
    val currentOnPhotos by rememberUpdatedState(onPhotos)
    val scope = rememberCoroutineScope()
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
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val copies = withContext(Dispatchers.IO) { uris.mapNotNull { context.copyToCache(it) } }
            if (copies.isNotEmpty()) currentOnPhotos(copies.map(Uri::toString))
        }
    }

    return remember(camera, picker) {
        PhotoSources(
            takePhoto = {
                val uri = context.newPhotoUri()
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

private fun Context.newPhotoFile(): File {
    val dir = File(cacheDir, PHOTO_DIR).apply { mkdirs() }
    return File.createTempFile("photo", ".jpg", dir)
}

private fun Context.newPhotoUri(): Uri = uriFor(newPhotoFile())

private fun Context.uriFor(file: File): Uri = FileProvider.getUriForFile(this, "$packageName$AUTHORITY_SUFFIX", file)

/**
 * Copies the picked photo's bytes unchanged (the EXIF orientation stays) and returns the copy's
 * URI, or null when the photo cannot be read. Blocking: call it off the main thread.
 */
private fun Context.copyToCache(source: Uri): Uri? {
    val file = newPhotoFile()
    return runCatching {
        val input = contentResolver.openInputStream(source) ?: error("No stream")
        input.use { from -> file.outputStream().use { to -> from.copyTo(to) } }
        uriFor(file)
    }.onFailure {
        file.delete()
        logcat(tag = TAG) { "Copy of $source failed: $it" }
    }.getOrNull()
}

private const val PREVIEW_LONG_SIDE_PX = 512
private const val PHOTO_DIR = "photos"

/** Must match the provider authority in this module's `AndroidManifest.xml`. */
private const val AUTHORITY_SUFFIX = ".photos"
private const val TAG = "PhotoSources"
