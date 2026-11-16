package com.grappim.koillectionmobile.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.customheaders.CustomHeader
import com.grappim.kit.customheaders.CustomHeaderValidator
import com.grappim.kit.customheaders.CustomHeadersStorage
import com.grappim.kit.domain.resultOf
import com.grappim.kit.logger.LogPriority
import com.grappim.kit.logger.logcat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Takes [CustomHeadersStorage] straight, with no repository between (one call on one storage
 * seam). A save needs nothing more: the `CustomHeaders` Ktor plugin reads the storage cache per
 * request, so the next request carries the new headers.
 *
 * Ported from wallosmobile's `CustomHeadersViewModel`.
 */
@KoinViewModel
class CustomHeadersViewModel(
    private val customHeadersStorage: CustomHeadersStorage,
    private val customHeaderValidator: CustomHeaderValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CustomHeadersUiState(
            onAdd = ::onAdd,
            onRemove = ::onRemove,
            onNameChange = ::onNameChange,
            onValueChange = ::onValueChange,
            onSaveClick = ::onSaveClick
        )
    )
    val uiState: StateFlow<CustomHeadersUiState> = _uiState.asStateFlow()

    init {
        customHeadersStorage.headers
            .onEach(::onStoredHeaders)
            .launchIn(viewModelScope)
    }

    /** The rows follow storage only while they still match what was saved: an edit in progress stays. */
    private fun onStoredHeaders(headers: List<CustomHeader>) {
        _uiState.update { state ->
            val rows = if (state.headers == state.saved) {
                headers.map { CustomHeaderRow(it.name, it.value) }
            } else {
                state.rows
            }
            state.copy(rows = rows, saved = headers)
        }
    }

    private fun onAdd() {
        updateRows { it + CustomHeaderRow() }
    }

    private fun onRemove(index: Int) {
        updateRows { rows -> rows.filterIndexed { i, _ -> i != index } }
    }

    private fun onNameChange(index: Int, name: String) {
        updateRows { rows -> rows.mapIndexed { i, row -> if (i == index) row.copy(name = name) else row } }
    }

    private fun onValueChange(index: Int, value: String) {
        updateRows { rows -> rows.mapIndexed { i, row -> if (i == index) row.copy(value = value) else row } }
    }

    /** Every edit validates the whole list again: a duplicate is a property of two rows, not one. */
    private fun updateRows(transform: (List<CustomHeaderRow>) -> List<CustomHeaderRow>) {
        val rows = transform(_uiState.value.rows)
        val errors = customHeaderValidator.validate(rows.map { CustomHeader(it.name.trim(), it.value.trim()) })
        _uiState.update { state ->
            state.copy(rows = rows.mapIndexed { i, row -> row.copy(error = errors[i]) })
        }
    }

    private fun onSaveClick() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return
        val headers = state.headers

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            resultOf { customHeadersStorage.setHeaders(headers) }
                .onSuccess {
                    // Unfilled rows were never part of the save. Dropping them makes the rows match
                    // storage again, and that turns Save off.
                    _uiState.update { current ->
                        current.copy(rows = headers.map { CustomHeaderRow(it.name, it.value) })
                    }
                }
                .onFailure {
                    // The rows keep what the user typed and Save stays on, so the user can try
                    // again. The log carries the throwable.
                    logcat(priority = LogPriority.WARN, throwable = it) { "Saving custom headers failed" }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
