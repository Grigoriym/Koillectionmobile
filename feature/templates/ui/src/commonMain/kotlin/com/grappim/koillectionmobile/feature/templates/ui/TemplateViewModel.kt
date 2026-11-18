package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceListChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.template_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.template_error_unexpected
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads template [templateId], its fields and the choice lists when the screen opens, again on
 * retry, and again when the template form saved it ([TemplateChanges]) or a choice list changed
 * ([ChoiceListChanges]: a renamed list shows its new name). [templateId] arrives
 * through `parametersOf` at the call site. The rows keep the repository's order: `getFields` sorts
 * by position.
 *
 * The delete asks first on the screen; [TemplateUiState.onDelete] is the confirmed delete. On
 * success the template list reads its list again ([TemplateChanges]).
 */
@KoinViewModel
class TemplateViewModel(
    @InjectedParam private val templateId: String,
    private val templatesRepository: TemplatesRepository,
    private val templateChanges: TemplateChanges,
    private val choiceListChanges: ChoiceListChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TemplateUiState(onRetry = ::retry, onDelete = ::delete, onDeleteErrorDismiss = ::dismissDeleteError)
    )
    val uiState: StateFlow<TemplateUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            templateChanges.changedTemplates.filter { it == templateId && !_uiState.value.isDeleted }.collect { load() }
        }
        viewModelScope.launch {
            choiceListChanges.changed.filter { !_uiState.value.isDeleted }.collect { load() }
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val template = async { templatesRepository.getTemplate(templateId) }
            val fields = async { templatesRepository.getFields(templateId) }
            val choiceLists = async { templatesRepository.getChoiceLists() }
            val templateResult = template.await()
            val fieldsResult = fields.await()
            val choiceListsResult = choiceLists.await()
            val failure = templateResult.exceptionOrNull()
                ?: fieldsResult.exceptionOrNull()
                ?: choiceListsResult.exceptionOrNull()
            if (failure != null) {
                _uiState.update {
                    it.copy(
                        fields = emptyList(),
                        isLoading = false,
                        error = failure.toMessage(RString.template_error_unexpected)
                    )
                }
                return@launch
            }
            val names = choiceListsResult.getOrThrow().associate { it.id to it.name }
            val rows = fieldsResult.getOrThrow().map { field ->
                TemplateFieldRow(
                    id = field.id,
                    name = field.name,
                    type = field.type,
                    choiceListName = field.choiceListId?.let(names::get)
                )
            }
            _uiState.update {
                it.copy(
                    name = templateResult.getOrThrow().name,
                    fields = rows,
                    isLoading = false,
                    error = NativeText.Empty
                )
            }
        }
    }

    private fun delete() {
        if (!_uiState.value.canChange) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = templatesRepository.deleteTemplate(templateId)
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.template_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
            // After isDeleted: this screen's own collector must not read the deleted template again.
            result.onSuccess { templateChanges.notifyChanged(templateId) }
        }
    }

    private fun dismissDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }
}
