package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceListChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.choice_lists_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the choice lists once when the screen opens, again on retry, and again when a choice list
 * changes ([ChoiceListChanges]) while the screen waits lower on the back stack.
 */
@KoinViewModel
class ChoiceListListViewModel(
    private val templatesRepository: TemplatesRepository,
    private val choiceListChanges: ChoiceListChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoiceListListUiState(onRetry = ::retry))
    val uiState: StateFlow<ChoiceListListUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch { choiceListChanges.changed.collect { load() } }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = templatesRepository.getChoiceLists()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { lists ->
                        state.copy(
                            choiceLists = lists.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            choiceLists = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.choice_lists_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
