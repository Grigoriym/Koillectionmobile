package com.grappim.koillectionmobile.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * The [error] with a retry button, or [emptyText] when there is no error. It scrolls, so a pull to
 * refresh around it works too.
 */
@Composable
fun EmptyOrError(
    error: NativeText,
    emptyText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryEnabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically)
    ) {
        if (error.isNotEmpty()) {
            Text(text = error.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Button(onClick = onRetry, enabled = retryEnabled) {
                Text(stringResource(RString.retry))
            }
        } else {
            Text(text = emptyText, textAlign = TextAlign.Center)
        }
    }
}

private val PADDING = 16.dp
