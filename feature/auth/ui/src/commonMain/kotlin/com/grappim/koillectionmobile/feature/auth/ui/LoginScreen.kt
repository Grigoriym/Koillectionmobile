package com.grappim.koillectionmobile.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.login_button
import com.grappim.koillectionmobile.strings.generated.resources.login_password_label
import com.grappim.koillectionmobile.strings.generated.resources.login_server_url_label
import com.grappim.koillectionmobile.strings.generated.resources.login_server_url_placeholder
import com.grappim.koillectionmobile.strings.generated.resources.login_title
import com.grappim.koillectionmobile.strings.generated.resources.login_username_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun LoginContent(uiState: LoginUiState, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val isEnabled = !uiState.isLoading

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING, Alignment.CenterVertically)
        ) {
            Text(text = stringResource(RString.login_title), style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.serverUrl,
                onValueChange = uiState.onServerUrlChange,
                label = { Text(stringResource(RString.login_server_url_label)) },
                placeholder = { Text(stringResource(RString.login_server_url_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                enabled = isEnabled
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Username },
                value = uiState.username,
                onValueChange = uiState.onUsernameChange,
                label = { Text(stringResource(RString.login_username_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                enabled = isEnabled
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
                value = uiState.password,
                onValueChange = uiState.onPasswordChange,
                label = { Text(stringResource(RString.login_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { uiState.onLogInClick() }),
                enabled = isEnabled
            )

            if (uiState.error.isNotEmpty()) {
                Text(
                    text = uiState.error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE))
            } else {
                Button(
                    onClick = uiState.onLogInClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canLogIn
                ) {
                    Text(stringResource(RString.login_button))
                }
            }
        }
    }
}

private val PADDING = 24.dp
private val FIELD_SPACING = 16.dp
private val PROGRESS_SIZE = 40.dp
