package com.grappim.koillectionmobile.feature.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.feature.loans.domain.LoanChanges
import com.grappim.koillectionmobile.feature.loans.domain.LoansRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.loans_error_unexpected
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * The loans section of the item screen. It reads the loans of item [itemId] on its own, so the
 * rest of the item screen does not wait for it. It reads again when a loan form saved a loan of
 * this item ([LoanChanges]).
 *
 * [itemId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class LoansViewModel(
    @InjectedParam private val itemId: String,
    private val repository: LoansRepository,
    private val loanChanges: LoanChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState(onRetry = ::retry))
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            loanChanges.changed.filter { it == itemId }.collect { reload() }
        }
    }

    /** A read that is still running started before the change, so it gives way to a new one. */
    private fun reload() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        loadJob = viewModelScope.launch {
            val result = repository.getLoans(itemId)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { loans ->
                        state.copy(loans = loans.sortedWith(LOAN_ORDER), isLoading = false, error = NativeText.Empty)
                    },
                    onFailure = {
                        state.copy(
                            loans = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.loans_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}

/** The loans that are not returned first, then by lent date, newest first. */
private val LOAN_ORDER: Comparator<Loan> =
    compareBy<Loan> { it.returnedAt != null }.thenByDescending { it.lentAt }
