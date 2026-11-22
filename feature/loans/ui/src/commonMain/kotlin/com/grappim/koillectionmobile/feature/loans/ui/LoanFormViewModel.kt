package com.grappim.koillectionmobile.feature.loans.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.loans.domain.LoanChanges
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_not_found
import com.grappim.koillectionmobile.strings.generated.resources.loan_form_save_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.loans_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

/**
 * The loan form of [route]: a new loan of the item, or the edit of one loan. A new loan starts
 * with an empty name and today as the lent date. The edit form reads the loans of the item and
 * takes the one with the route's id (the repository has no call for one loan).
 *
 * The save is one call: a `POST` for a new loan, or a `PATCH` with only the changed fields
 * ([LoanFormValues.changesFrom]) for an edit. On success the loans section reads again
 * ([LoanChanges]).
 *
 * The start values and the edits are kept in [savedStateHandle], so they survive process death and
 * the edit form does not read the loan again. A save that a process death stopped is not kept: the
 * form shows again and Save sends it again.
 *
 * [route] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin compiler
 * plugin not to look for it in the graph.
 */
@KoinViewModel
class LoanFormViewModel(
    @InjectedParam private val route: LoanFormRoute,
    private val repository: LoansRepository,
    private val loanChanges: LoanChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: LoanForm? = savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<LoanForm>(it) }

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<LoanFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { s -> s.original?.let { original -> s.values?.let { LoanForm(original, it) } } }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { savedStateHandle[FORM_KEY] = json.encodeToString(it) }
        }
        if (_uiState.value.original == null) load()
    }

    private fun initialState(): LoanFormUiState {
        val start = saved?.original ?: if (route.loanId == null) LoanFormValues(lentAt = today()) else null
        return LoanFormUiState(
            isEdit = route.loanId != null,
            original = start,
            values = saved?.values ?: start,
            isLoading = start == null,
            isSaved = savedStateHandle.get<Boolean>(SAVED_KEY) == true,
            onRetry = ::retry,
            onLentToChange = { text -> updateValues { it.copy(lentTo = text) } },
            onLentAtChange = { date -> updateValues { it.copy(lentAt = date) } },
            onReturnedAtChange = { date -> updateValues { it.copy(returnedAt = date) } },
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm
        )
    }

    private fun updateValues(change: (LoanFormValues) -> LoanFormValues) {
        _uiState.update { s -> s.copy(values = s.values?.let(change)) }
    }

    private fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.original != null) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        val loanId = route.loanId ?: return
        viewModelScope.launch {
            repository.getLoans(route.itemId)
                .onSuccess { loans ->
                    val loan = loans.find { it.id == loanId }
                    _uiState.update {
                        if (loan == null) {
                            it.copy(isLoading = false, error = NativeText.Resource(RString.loan_form_not_found))
                        } else {
                            val values = loan.toFormValues()
                            it.copy(original = values, values = values, isLoading = false, error = NativeText.Empty)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toMessage(RString.loans_error_unexpected)) }
                }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave || state.save != null) return
        runSave()
    }

    private fun retrySave() {
        if (_uiState.value.save !is LoanSaveStatus.Failed) return
        runSave()
    }

    private fun editForm() {
        if (_uiState.value.save !is LoanSaveStatus.Failed) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val state = _uiState.value
        val values = state.values ?: return
        _uiState.update { it.copy(save = LoanSaveStatus.Sending) }
        viewModelScope.launch {
            val loanId = route.loanId
            val result = if (loanId == null) {
                repository.createLoan(route.itemId, values.lentTo.trim(), values.lentAt)
            } else {
                repository.editLoan(loanId, state.changes)
            }
            result
                .onSuccess {
                    savedStateHandle[SAVED_KEY] = true
                    loanChanges.notifyChanged(route.itemId)
                    _uiState.update { it.copy(isSaved = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(save = LoanSaveStatus.Failed(e.toMessage(RString.loan_form_save_error_unexpected)))
                    }
                }
        }
    }
}

/** Today on this device, the default lent date of a new loan. */
internal fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** What [LoanFormViewModel] keeps of the form across process death. */
@Serializable
private data class LoanForm(val original: LoanFormValues, val values: LoanFormValues)

private const val FORM_KEY = "loan_form"
private const val SAVED_KEY = "loan_form_saved"

private val json = Json { ignoreUnknownKeys = true }
