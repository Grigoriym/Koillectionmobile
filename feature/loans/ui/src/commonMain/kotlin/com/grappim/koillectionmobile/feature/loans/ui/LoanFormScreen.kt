package com.grappim.koillectionmobile.feature.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionTextButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_discard
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_edit_form
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_keep_editing
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_retry
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_clear
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_discard_title
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_edit_discard_text
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_edit_title
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_lent_at
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_lent_to
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_new_title
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_pick_cancel
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_pick_ok
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_returned_at
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_saving
import com.grappim.koillectionmobile.strings.generated.resources.loans_not_returned
import com.grappim.koillectionmobile.strings.generated.resources.loans_retry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

/**
 * The loan form of [route]. While the user changed something, the back arrow and the system back
 * ask first. [onClose] runs once the loan is saved, to take the form off the back stack.
 */
@Composable
fun LoanFormScreen(
    route: LoanFormRoute,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanFormViewModel = koinViewModel { parametersOf(route) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) currentOnClose()
    }
    var isDiscardShown by rememberSaveable { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.hasInput,
        onBackCompleted = { isDiscardShown = true }
    )
    if (isDiscardShown) {
        DiscardDialog(isEdit = uiState.isEdit, onDismiss = { isDiscardShown = false }, onDiscard = {
            isDiscardShown = false
            onBack()
        })
    }
    val topBarController = LocalTopBarConfig.current
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSave by rememberUpdatedState(uiState.onSave)
    val hasInput = uiState.hasInput
    val isSaveShown = uiState.values != null && uiState.save == null
    val canSave = uiState.canSave
    LaunchedEffect(uiState.isEdit, hasInput, isSaveShown, canSave) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(
                    if (uiState.isEdit) RString.loan_form_edit_title else RString.loan_form_new_title
                ),
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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        val values = uiState.values
        val save = uiState.save
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            values == null -> Column(
                modifier = contentModifier.padding(PADDING),
                verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = uiState.error.asString(), color = MaterialTheme.colorScheme.error)
                Button(onClick = uiState.onRetry) { Text(stringResource(RString.loans_retry)) }
            }

            save != null -> SaveView(
                status = save,
                onRetry = uiState.onRetrySave,
                onEditForm = uiState.onEditForm,
                modifier = contentModifier
            )

            else -> LoanForm(
                values = values,
                isEdit = uiState.isEdit,
                onLentToChange = uiState.onLentToChange,
                onLentAtChange = uiState.onLentAtChange,
                onReturnedAtChange = uiState.onReturnedAtChange,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun DiscardDialog(isEdit: Boolean, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isEdit) RString.collection_form_edit_discard_title else RString.loan_form_discard_title
                )
            )
        },
        text = {
            Text(stringResource(if (isEdit) RString.loan_form_edit_discard_text else RString.loan_form_discard_text))
        },
        confirmButton = {
            TextButton(onClick = onDiscard) { Text(stringResource(RString.collection_form_discard)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.collection_form_keep_editing)) }
        }
    )
}

/** The name and the dates. The returned date is on the edit form only. */
@Composable
private fun LoanForm(
    values: LoanFormValues,
    isEdit: Boolean,
    onLentToChange: (String) -> Unit,
    onLentAtChange: (LocalDate) -> Unit,
    onReturnedAtChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(PADDING),
        verticalArrangement = Arrangement.spacedBy(PADDING)
    ) {
        OutlinedTextField(
            value = values.lentTo,
            onValueChange = onLentToChange,
            label = { Text(stringResource(RString.loan_form_lent_to)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        DateField(
            label = stringResource(RString.loan_form_lent_at),
            date = values.lentAt,
            emptyText = "",
            onPick = onLentAtChange,
            onClear = null
        )
        if (isEdit) {
            DateField(
                label = stringResource(RString.loan_form_returned_at),
                date = values.returnedAt,
                emptyText = stringResource(RString.loans_not_returned),
                onPick = onReturnedAtChange,
                onClear = { onReturnedAtChange(null) }
            )
        }
    }
}

/**
 * A date as `yyyy-MM-dd` on a button that opens the date picker. With [onClear], a set date has a
 * clear button; [emptyText] shows while no date is set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    emptyText: String,
    onPick: (LocalDate) -> Unit,
    onClear: (() -> Unit)?
) {
    var isPickerShown by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(PADDING / 2)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(PADDING / 2), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { isPickerShown = true }) { Text(date?.toString() ?: emptyText) }
            if (onClear != null && date != null) {
                TextButton(onClick = onClear) { Text(stringResource(RString.loan_form_clear)) }
            }
        }
    }
    if (isPickerShown) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date?.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { isPickerShown = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(it.toPickerDate()) }
                    isPickerShown = false
                }) { Text(stringResource(RString.loan_form_pick_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { isPickerShown = false }) { Text(stringResource(RString.loan_form_pick_cancel)) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** The save instead of the form: a progress view while it runs; on a failure the message, a retry and the way back to the form. */
@Composable
private fun SaveView(
    status: LoanSaveStatus,
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
            LoanSaveStatus.Sending -> {
                CircularProgressIndicator()
                Text(stringResource(RString.loan_form_saving))
            }

            is LoanSaveStatus.Failed -> {
                Text(text = status.message.asString(), color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text(stringResource(RString.collection_form_retry)) }
                OutlinedButton(onClick = onEditForm) { Text(stringResource(RString.collection_form_edit_form)) }
            }
        }
    }
}

/** The date picker works in UTC midnight milliseconds. */
private fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toPickerDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

private val PADDING = 16.dp
