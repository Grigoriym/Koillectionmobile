package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceListChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_delete_error_in_use
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.choice_list_form_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * The choice list form: a new choice list when [choiceListId] is null, else the edit form of that
 * list. It edits the name and the choices (add, remove, move up and down). The repository has no
 * single read, so the edit form reads all choice lists and takes [choiceListId].
 *
 * The list is read once. The loaded values and the edits are kept in [savedStateHandle], so after a
 * process death the form shows the edits and does not read the list again.
 *
 * The save is one call: the whole list, names trimmed. The delete asks first on the screen;
 * [ChoiceListFormUiState.onDelete] is the confirmed delete. After a save or a delete the screens
 * that show choice lists read them again ([ChoiceListChanges]).
 */
@KoinViewModel
class ChoiceListFormViewModel(
    @InjectedParam private val choiceListId: String?,
    private val templatesRepository: TemplatesRepository,
    private val choiceListChanges: ChoiceListChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: SavedChoiceListForm? =
        savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<SavedChoiceListForm>(it) }

    private var nextId = saved?.draft?.choices?.maxOfOrNull { it.id }?.plus(1) ?: 0

    private val _uiState = MutableStateFlow(
        ChoiceListFormUiState(
            isNew = choiceListId == null,
            draft = saved?.draft ?: EMPTY.takeIf { choiceListId == null },
            original = saved?.original ?: EMPTY.takeIf { choiceListId == null },
            isLoading = saved == null && choiceListId != null,
            onRetry = ::retry,
            onNameChange = { name -> updateDraft { it.copy(name = name) } },
            onAddChoice = ::addChoice,
            onRemoveChoice = { id -> updateDraft { d -> d.copy(choices = d.choices.filterNot { it.id == id }) } },
            onChoiceChange = { id, value ->
                updateDraft { d -> d.copy(choices = d.choices.map { if (it.id == id) it.copy(value = value) else it }) }
            },
            onMoveUp = { move(it, -1) },
            onMoveDown = { move(it, 1) },
            onSave = ::save,
            onDelete = ::delete,
            onActionErrorDismiss = ::dismissActionError
        )
    )
    val uiState: StateFlow<ChoiceListFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { state ->
                    state.original?.let { original -> state.draft?.let { SavedChoiceListForm(original, it) } }
                }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { savedStateHandle[FORM_KEY] = json.encodeToString(it) }
        }
        if (_uiState.value.isLoading) load()
    }

    private fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.error.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        val id = choiceListId ?: return
        viewModelScope.launch {
            val result = templatesRepository.getChoiceLists().mapCatching { lists ->
                lists.firstOrNull { it.id == id } ?: throw KoillectionError.NotFound("choice list $id")
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { list ->
                        val loaded = ChoiceListDraft(list.name, list.choices.map { FormChoice(nextId++, it) })
                        state.copy(draft = loaded, original = loaded, isLoading = false, error = NativeText.Empty)
                    },
                    onFailure = {
                        state.copy(isLoading = false, error = it.toMessage(RString.choice_list_form_error_unexpected))
                    }
                )
            }
        }
    }

    private fun dismissActionError() {
        _uiState.update { it.copy(actionError = NativeText.Empty) }
    }

    private fun addChoice() = updateDraft { it.copy(choices = it.choices + FormChoice(nextId++, "")) }

    /** Moves choice [id] by [offset] rows; a move past either end does nothing. */
    private fun move(id: Int, offset: Int) = updateDraft { d ->
        val from = d.choices.indexOfFirst { it.id == id }
        val to = from + offset
        if (from < 0 || to !in d.choices.indices) {
            d
        } else {
            d.copy(choices = d.choices.toMutableList().apply { add(to, removeAt(from)) })
        }
    }

    private fun updateDraft(change: (ChoiceListDraft) -> ChoiceListDraft) {
        _uiState.update { state ->
            if (state.isBusy) state else state.draft?.let { state.copy(draft = change(it)) } ?: state
        }
    }

    private fun save() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, actionError = NativeText.Empty) }
        val name = draft.name.trim()
        val choices = draft.choices.map { it.value.trim() }
        viewModelScope.launch {
            val result = if (choiceListId == null) {
                templatesRepository.createChoiceList(name, choices)
            } else {
                templatesRepository.editChoiceList(choiceListId, name, choices)
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    isDone = result.isSuccess,
                    actionError = result.exceptionOrNull()?.toSaveMessage() ?: NativeText.Empty
                )
            }
            result.onSuccess { choiceListChanges.notifyChanged() }
        }
    }

    private fun delete() {
        val id = choiceListId ?: return
        if (!_uiState.value.canDelete) return
        _uiState.update { it.copy(isDeleting = true, actionError = NativeText.Empty) }
        viewModelScope.launch {
            val result = templatesRepository.deleteChoiceList(id)
            _uiState.update {
                it.copy(
                    isDeleting = false,
                    isDone = result.isSuccess,
                    actionError = result.exceptionOrNull()?.toDeleteMessage() ?: NativeText.Empty
                )
            }
            result.onSuccess { choiceListChanges.notifyChanged() }
        }
    }
}

/**
 * A choice list that a template field or an item field uses: the server's foreign key refuses the
 * delete and it answers 500 (Koillection 1.8.5, checked in M8.5).
 */
private fun Throwable.toDeleteMessage(): NativeText = when (this) {
    is KoillectionError.Server -> NativeText.Arguments(RString.choice_list_delete_error_in_use, listOf(status))
    else -> toMessage(RString.choice_list_delete_error_unexpected)
}

/** What [ChoiceListFormViewModel] keeps of the form across process death. */
@Serializable
private data class SavedChoiceListForm(val original: ChoiceListDraft, val draft: ChoiceListDraft)

private val EMPTY = ChoiceListDraft(name = "", choices = emptyList())

private const val FORM_KEY = "choice_list_form"

private val json = Json { ignoreUnknownKeys = true }
