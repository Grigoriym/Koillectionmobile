package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_retry
import org.jetbrains.compose.resources.stringResource

/** The [error] with a retry button, or [emptyText] when there is no error. */
@Composable
internal fun EmptyOrError(error: NativeText, emptyText: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically)
    ) {
        if (error.isNotEmpty()) {
            Text(text = error.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Button(onClick = onRetry) {
                Text(stringResource(RString.wishlists_retry))
            }
        } else {
            Text(text = emptyText, textAlign = TextAlign.Center)
        }
    }
}

/** A row image for a wishlist or a wish. Without an image, the first letter of the name. */
@Composable
internal fun Thumbnail(imageUrl: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(IMAGE_SIZE)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal val PADDING = 16.dp
private val IMAGE_SIZE = 48.dp
