package com.grappim.koillectionmobile.core.form

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionTextButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

/**
 * The frame of a name-and-image form, new and edit: the top bar with Save, and the discard confirm.
 * While [hasInput], one back tap must not lose the form: the arrow and the system back ask first.
 * [content] gets the modifier of the area under the top bar.
 */
@Composable
fun FormScaffold(
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
