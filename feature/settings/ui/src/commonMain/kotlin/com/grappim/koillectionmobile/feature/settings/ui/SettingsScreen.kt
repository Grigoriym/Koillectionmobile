package com.grappim.koillectionmobile.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.customheaders.CustomHeaderError
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_add
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_description
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_error_duplicate_name
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_error_invalid_name
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_error_invalid_value
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_error_missing_name
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_error_reserved_name
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_name_label
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_name_placeholder
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_remove
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_save
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_title
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_value_hint
import com.grappim.koillectionmobile.strings.generated.resources.custom_headers_value_label
import com.grappim.koillectionmobile.strings.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Today Settings holds one section: the custom headers. The app opens it as [SettingsRoute], a
 * drawer section, with [NavigationIconConfig.Menu]. The login screen opens it as state, with
 * [NavigationIconConfig.Back]. The top bar is the caller's: this screen sets its title and
 * [navigationIcon] through `LocalTopBarConfig`.
 */
@Composable
fun SettingsScreen(
    navigationIcon: NavigationIconConfig,
    modifier: Modifier = Modifier,
    viewModel: CustomHeadersViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarController = LocalTopBarConfig.current

    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(title = NativeText.Resource(RString.settings_title), navigationIcon = navigationIcon)
        )
    }

    CustomHeadersSection(
        uiState = uiState,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    )
}

@Composable
private fun CustomHeadersSection(uiState: CustomHeadersUiState, modifier: Modifier = Modifier) {
    val enabled = !uiState.isSaving

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        Text(text = stringResource(RString.custom_headers_title), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(RString.custom_headers_description), style = MaterialTheme.typography.bodyMedium)

        uiState.rows.forEachIndexed { index, row ->
            CustomHeaderRowFields(
                row = row,
                enabled = enabled,
                onNameChange = { uiState.onNameChange(index, it) },
                onValueChange = { uiState.onValueChange(index, it) },
                onRemove = { uiState.onRemove(index) }
            )
        }

        TextButton(onClick = uiState.onAdd, enabled = enabled) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(stringResource(RString.custom_headers_add))
        }

        if (uiState.isSaving) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = uiState.onSaveClick,
                enabled = uiState.isSaveEnabled
            ) {
                Text(stringResource(RString.custom_headers_save))
            }
        }
    }
}

@Composable
private fun CustomHeaderRowFields(
    row: CustomHeaderRow,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val error = row.error
    val isValueError = error == CustomHeaderError.InvalidValue

    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = row.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(RString.custom_headers_name_label)) },
                placeholder = { Text(stringResource(RString.custom_headers_name_placeholder)) },
                isError = error != null && !isValueError,
                supportingText = error?.takeUnless { isValueError }?.let { { Text(stringResource(it.message())) } },
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = row.value,
                onValueChange = onValueChange,
                label = { Text(stringResource(RString.custom_headers_value_label)) },
                isError = isValueError,
                supportingText = {
                    Text(
                        stringResource(
                            error?.takeIf { isValueError }?.message() ?: RString.custom_headers_value_hint
                        )
                    )
                },
                singleLine = true,
                enabled = enabled
            )
        }
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(RString.custom_headers_remove))
        }
    }
}

private fun CustomHeaderError.message(): StringResource = when (this) {
    CustomHeaderError.MissingName -> RString.custom_headers_error_missing_name
    CustomHeaderError.InvalidName -> RString.custom_headers_error_invalid_name
    CustomHeaderError.ReservedName -> RString.custom_headers_error_reserved_name
    CustomHeaderError.DuplicateName -> RString.custom_headers_error_duplicate_name
    CustomHeaderError.InvalidValue -> RString.custom_headers_error_invalid_value
}

private val SCREEN_PADDING = 16.dp
private val ROW_SPACING = 8.dp
private val FIELD_SPACING = 4.dp
