package com.grappim.koillectionmobile.feature.items.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grappim.koillectionmobile.core.image.Rotation

@Composable
internal actual fun rememberPhotoSources(onPhotos: (List<String>) -> Unit): PhotoSources? = null

@Composable
internal actual fun PhotoPreview(uri: String, rotation: Rotation, modifier: Modifier) {
    Box(modifier = modifier)
}
