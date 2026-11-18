package com.grappim.koillectionmobile.feature.templates.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.EditedTemplateField
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateProgress
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateResult
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateUseCase
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.feature.templates.domain.TemplateEdit
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplateSnapshot
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_validation
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.template_form_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.template_form_save_error_duplicate_name
import kotlinx.coroutines.async
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
 * The template form: a new template when [templateId] is null, else the edit form of that template.
 * It edits the name and the field rows (name, type, choice list; add, remove, move up and down).
 *
 * The template is read once. The loaded values and the edits are kept in [savedStateHandle], so
 * after a process death the form shows the edits and does not read the template again. The choice
 * lists are read each time the form starts.
 *
 * The save goes through [SaveTemplateUseCase]: the first try is [SaveTemplateUseCase.save]; every
 * later one, after a process death too, is [SaveTemplateUseCase.retry] once the template id is
 * known, which reads the template again and sends only what still differs. On success the template
 * screens read the template again ([TemplateChanges]).
 */
@KoinViewModel
class TemplateFormViewModel(
    @InjectedParam private val templateId: String?,
    private val templatesRepository: TemplatesRepository,
    private val saveTemplateUseCase: SaveTemplateUseCase,
    private val templateChanges: TemplateChanges,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saved: SavedForm? = savedStateHandle.get<String>(FORM_KEY)?.let { json.decodeFromString<SavedForm>(it) }

    /** The template as loaded: the base of the save's diff. Null for a new template. */
    private var snapshot: TemplateSnapshot? =
        savedStateHandle.get<String>(SNAPSHOT_KEY)?.let { json.decodeFromString<SavedSnapshot>(it).toSnapshot() }

    private var record: SaveRecord =
        savedStateHandle.get<String>(SAVE_KEY)?.let { json.decodeFromString<SaveRecord>(it) } ?: SaveRecord()

    /**
     * A call of an earlier try went through, so the form can no longer change (only a retry).
     * After a process death that is not known, so a started save counts as sent.
     */
    private var hasSent = record.isTried

    private var nextId = saved?.draft?.fields?.maxOfOrNull { it.id }?.plus(1) ?: 0

    private val _uiState = MutableStateFlow(
        TemplateFormUiState(
            isNew = templateId == null,
            draft = saved?.draft ?: EMPTY.takeIf { templateId == null },
            original = saved?.original ?: EMPTY.takeIf { templateId == null },
            savedTemplateId = record.savedId,
            onRetry = ::retry,
            onNameChange = ::changeName,
            onAddField = ::addField,
            onRemoveField = ::removeField,
            onFieldNameChange = { id, name -> updateField(id) { it.copy(name = name) } },
            onFieldTypeChange = { id, type -> updateField(id) { it.copy(type = type) } },
            onFieldChoiceListChange = { id, choiceListId -> updateField(id) { it.copy(choiceListId = choiceListId) } },
            onMoveUp = { move(it, -1) },
            onMoveDown = { move(it, 1) },
            onSave = ::save,
            onRetrySave = ::retrySave,
            onEditForm = ::editForm,
            // A save that a process death stopped: what went through is not known, so only a retry.
            save = if (record.isTried && record.savedId == null) {
                TemplateSaveStatus.Failed(
                    NativeText.Resource(RString.collection_form_save_unfinished),
                    isPartial = true
                )
            } else {
                null
            }
        )
    )
    val uiState: StateFlow<TemplateFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { state -> state.original?.let { original -> state.draft?.let { SavedForm(original, it) } } }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { savedStateHandle[FORM_KEY] = json.encodeToString(it) }
        }
        load()
    }

    private fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.error.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    /** The choice lists, and the template with its fields when the form has no draft yet. */
    private fun load() {
        viewModelScope.launch {
            val id = templateId.takeIf { _uiState.value.draft == null }
            val choiceLists = async { templatesRepository.getChoiceLists() }
            val template = id?.let { async { templatesRepository.getTemplate(it) } }
            val fields = id?.let { async { templatesRepository.getFields(it) } }
            val choiceListsResult = choiceLists.await()
            val templateResult = template?.await()
            val fieldsResult = fields?.await()
            val failure = choiceListsResult.exceptionOrNull()
                ?: templateResult?.exceptionOrNull()
                ?: fieldsResult?.exceptionOrNull()
            if (failure != null) {
                _uiState.update {
                    it.copy(isLoading = false, error = failure.toMessage(RString.template_form_error_unexpected))
                }
                return@launch
            }
            val loaded = if (templateResult != null && fieldsResult != null) {
                val loadedSnapshot = TemplateSnapshot(templateResult.getOrThrow(), fieldsResult.getOrThrow())
                snapshot = loadedSnapshot
                savedStateHandle[SNAPSHOT_KEY] = json.encodeToString(SavedSnapshot.of(loadedSnapshot))
                TemplateDraft(
                    name = loadedSnapshot.template.name,
                    fields = loadedSnapshot.fields.map {
                        FormField(
                            id = nextId++,
                            fieldId = it.id,
                            name = it.name,
                            type = it.type,
                            choiceListId = it.choiceListId
                        )
                    }
                )
            } else {
                null
            }
            _uiState.update { state ->
                state.copy(
                    draft = state.draft ?: loaded,
                    original = state.original ?: loaded,
                    choiceLists = choiceListsResult.getOrThrow().sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER) {
                            it.name
                        }
                    ),
                    isLoading = false,
                    error = NativeText.Empty
                )
            }
        }
    }

    private fun changeName(name: String) = updateDraft { it.copy(name = name) }

    private fun addField() = updateDraft { it.copy(fields = it.fields + FormField(nextId++, null, "", TYPE_TEXT)) }

    private fun removeField(id: Int) = updateDraft { d -> d.copy(fields = d.fields.filterNot { it.id == id }) }

    /**
     * A type change keeps the row's choice list, so a change back to `choice-list` restores it. The
     * save sends a choice list only for a `choice-list` field.
     */
    private fun updateField(id: Int, change: (FormField) -> FormField) = updateDraft { d ->
        d.copy(fields = d.fields.map { if (it.id == id) change(it) else it })
    }

    /** Moves field [id] by [offset] rows; a move past either end does nothing. */
    private fun move(id: Int, offset: Int) = updateDraft { d ->
        val from = d.fields.indexOfFirst { it.id == id }
        val to = from + offset
        if (from < 0 || to !in d.fields.indices) {
            d
        } else {
            d.copy(fields = d.fields.toMutableList().apply { add(to, removeAt(from)) })
        }
    }

    private fun updateDraft(change: (TemplateDraft) -> TemplateDraft) {
        _uiState.update { state ->
            if (state.save != null) state else state.draft?.let { state.copy(draft = change(it)) } ?: state
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave || state.save != null) return
        runSave()
    }

    private fun retrySave() {
        if (_uiState.value.save !is TemplateSaveStatus.Failed) return
        runSave()
    }

    /** Only when no call went through: the form can still change. */
    private fun editForm() {
        val status = _uiState.value.save
        if (status !is TemplateSaveStatus.Failed || status.isPartial) return
        _uiState.update { it.copy(save = null) }
    }

    private fun runSave() {
        val draft = _uiState.value.draft ?: return
        val edit = draft.toEdit()
        val knownId = templateId ?: record.createdId
        val isRetry = record.isTried && knownId != null
        keepRecord(record.copy(isTried = true))
        _uiState.update { it.copy(save = TemplateSaveStatus.Sending(0, 0)) }
        viewModelScope.launch {
            val onProgress: (SaveTemplateProgress) -> Unit = { progress ->
                if (progress.done > 0) hasSent = true
                _uiState.update { it.copy(save = TemplateSaveStatus.Sending(progress.done, progress.total)) }
            }
            val result = if (isRetry) {
                saveTemplateUseCase.retry(checkNotNull(knownId), snapshot, edit, onProgress)
            } else {
                saveTemplateUseCase.save(snapshot, edit, onProgress)
            }
            when (result) {
                is SaveTemplateResult.Success -> {
                    keepRecord(record.copy(createdId = result.templateId, savedId = result.templateId))
                    templateChanges.notifyChanged(result.templateId)
                    _uiState.update { it.copy(savedTemplateId = result.templateId) }
                }

                is SaveTemplateResult.DuplicateName ->
                    fail(NativeText.Arguments(RString.template_form_save_error_duplicate_name, listOf(result.name)))

                is SaveTemplateResult.Failure -> {
                    if (templateId == null && result.templateId != null) {
                        keepRecord(record.copy(createdId = result.templateId))
                    }
                    fail(result.error.toSaveMessage())
                }
            }
        }
    }

    private fun fail(message: NativeText) {
        _uiState.update { it.copy(save = TemplateSaveStatus.Failed(message, isPartial = hasSent)) }
    }

    private fun keepRecord(newRecord: SaveRecord) {
        record = newRecord
        savedStateHandle[SAVE_KEY] = json.encodeToString(newRecord)
    }
}

/** The form as the use case takes it: names trimmed. */
private fun TemplateDraft.toEdit() = TemplateEdit(
    name = name.trim(),
    fields = fields.map { EditedTemplateField(it.fieldId, it.name.trim(), it.type, it.choiceListId) }
)

private fun Throwable.toSaveMessage(): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.collection_form_save_error_network)
    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
    is KoillectionError.Server -> NativeText.Arguments(RString.collection_form_save_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.collection_form_save_error_http, listOf(status))
    is KoillectionError.Validation -> NativeText.Resource(RString.collection_form_save_error_validation)
    else -> NativeText.Resource(RString.collection_form_save_error_unexpected)
}

/** What [TemplateFormViewModel] keeps of the form across process death. */
@Serializable
private data class SavedForm(val original: TemplateDraft, val draft: TemplateDraft)

/**
 * What [TemplateFormViewModel] keeps of a save across process death: [isTried] once a save began,
 * [createdId] once the server has the new template, and [savedId] once every call went through.
 */
@Serializable
private data class SaveRecord(val isTried: Boolean = false, val createdId: String? = null, val savedId: String? = null)

/** [TemplateSnapshot] in saved state: the domain classes are not serializable. */
@Serializable
private data class SavedSnapshot(val id: String, val name: String, val fields: List<SavedField>) {
    fun toSnapshot() = TemplateSnapshot(
        template = Template(id, name),
        fields = fields.map { TemplateField(it.id, it.name, it.position, it.type, it.choiceListId) }
    )

    companion object {
        fun of(snapshot: TemplateSnapshot) = SavedSnapshot(
            id = snapshot.template.id,
            name = snapshot.template.name,
            fields = snapshot.fields.map { SavedField(it.id, it.name, it.position, it.type, it.choiceListId) }
        )
    }
}

@Serializable
private data class SavedField(
    val id: String,
    val name: String,
    val position: Int,
    val type: String,
    val choiceListId: String?
)

private val EMPTY = TemplateDraft(name = "", fields = emptyList())

private const val FORM_KEY = "template_form"
private const val SNAPSHOT_KEY = "template_form_snapshot"
private const val SAVE_KEY = "template_form_save"

private val json = Json { ignoreUnknownKeys = true }
