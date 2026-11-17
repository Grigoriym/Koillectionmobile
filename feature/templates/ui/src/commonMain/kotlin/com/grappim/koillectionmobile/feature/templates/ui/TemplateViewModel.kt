package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.template_error_unexpected
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the fields of template [templateId] and the choice lists when the screen opens, and again
 * on retry. [templateId] arrives through `parametersOf` at the call site. The rows keep the
 * repository's order: `getFields` sorts by position.
 */
@KoinViewModel
class TemplateViewModel(
    @InjectedParam private val templateId: String,
    private val templatesRepository: TemplatesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateUiState(onRetry = ::retry))
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val fields = async { templatesRepository.getFields(templateId) }
            val choiceLists = async { templatesRepository.getChoiceLists() }
            val fieldsResult = fields.await()
            val choiceListsResult = choiceLists.await()
            val failure = fieldsResult.exceptionOrNull() ?: choiceListsResult.exceptionOrNull()
            val result = if (failure != null) {
                Result.failure(failure)
            } else {
                val names = choiceListsResult.getOrThrow().associate { it.id to it.name }
                Result.success(
                    fieldsResult.getOrThrow().map { field ->
                        TemplateFieldRow(
                            id = field.id,
                            name = field.name,
                            type = field.type,
                            choiceListName = field.choiceListId?.let(names::get)
                        )
                    }
                )
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(fields = it, isLoading = false, error = NativeText.Empty) },
                    onFailure = {
                        state.copy(
                            fields = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.template_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
