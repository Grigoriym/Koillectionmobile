package com.grappim.koillectionmobile.core.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_form
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_preparing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_retry
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_sending
import org.jetbrains.compose.resources.stringResource

/**
 * The save instead of the form: a progress bar while it runs; on a failure the message and a
 * retry. The way back to the form is offered only while no call went through; otherwise
 * [partialText] says what is on the server.
 */
@Composable
fun FormSaveView(
    status: FormSaveStatus,
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
            FormSaveStatus.Preparing -> {
                CircularProgressIndicator()
                Text(stringResource(RString.collection_form_preparing))
            }

            is FormSaveStatus.Sending -> {
                LinearProgressIndicator(
                    progress = { status.done.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(RString.collection_form_sending, status.done, status.total))
            }

            is FormSaveStatus.Failed -> {
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

private val PADDING = 16.dp
