package com.grappim.koillectionmobile.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_ok
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The two dialogs of a nested delete. While [summary] is set: the confirm, with what the server
 * deletes with [name]. While [error] is set: the failure.
 */
@Composable
fun DeleteDialogs(
    summary: DeleteSummary?,
    error: NativeText,
    name: String,
    texts: DeleteTexts,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    summary?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(texts.title)) },
            text = {
                val children = pluralStringResource(texts.children, it.childCount, it.childCount)
                val contents = pluralStringResource(texts.contents, it.contentCount, it.contentCount)
                Text(stringResource(texts.text, name, children, contents))
            },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(stringResource(RString.collection_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(RString.collection_delete_cancel)) }
            }
        )
    }
    if (error.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text(stringResource(texts.failedTitle)) },
            text = { Text(error.asString()) },
            confirmButton = {
                TextButton(onClick = onErrorDismiss) { Text(stringResource(RString.collection_delete_failed_ok)) }
            }
        )
    }
}
