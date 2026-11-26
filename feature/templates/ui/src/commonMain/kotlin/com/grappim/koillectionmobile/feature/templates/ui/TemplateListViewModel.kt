package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the templates once when the screen opens, again on retry, and again when a template
 * changes ([TemplateChanges]) while the screen waits lower on the back stack.
 */
@KoinViewModel
class TemplateListViewModel(
    private val templatesRepository: TemplatesRepository,
    private val templateChanges: TemplateChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState(onRetry = ::retry))
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch { templateChanges.changedTemplates.collect { load() } }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = templatesRepository.getTemplates()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { templates ->
                        state.copy(
                            templates = templates.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            templates = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.templates_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
