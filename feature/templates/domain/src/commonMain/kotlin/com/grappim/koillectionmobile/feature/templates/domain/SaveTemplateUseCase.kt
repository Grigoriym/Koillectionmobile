package com.grappim.koillectionmobile.feature.templates.domain

/** A template with its fields (sorted by position), as read from the server. */
data class TemplateSnapshot(val template: Template, val fields: List<TemplateField>)

/**
 * The template form's values. [fields] is the final field list in the order the user wants: the
 * field at index `i` gets position `i + 1`. A field of the template that is not in [fields] is
 * deleted.
 */
data class TemplateEdit(val name: String, val fields: List<EditedTemplateField>)

/**
 * One field of the form. [fieldId] is the server id of an existing field; null for a new field.
 * [choiceListId] is used only for a `choice-list` [type].
 */
data class EditedTemplateField(val fieldId: String?, val name: String, val type: String, val choiceListId: String?)

/** [done] of [total] server calls are finished. */
data class SaveTemplateProgress(val done: Int, val total: Int)

sealed interface SaveTemplateResult {
    data class Success(val templateId: String) : SaveTemplateResult

    /** No call was sent: two fields would have [name]. The server does not check this (plan §2, M8.2). */
    data class DuplicateName(val name: String) : SaveTemplateResult

    /**
     * Some calls may have gone through: retry with [SaveTemplateUseCase.retry]. [templateId] is
     * the template the calls went to; null when the template could not be created.
     */
    data class Failure(val error: Throwable, val templateId: String?) : SaveTemplateResult
}

interface SaveTemplateUseCase {

    /**
     * Sends [edit]. With a null [original] it creates the template first; otherwise it sends only
     * what [edit] changes in [original] (the template as the form loaded it). Order: field
     * deletes, the rename, field changes (name, type, choice list and position in one call per
     * field), new fields.
     */
    suspend fun save(
        original: TemplateSnapshot?,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit = {}
    ): SaveTemplateResult

    /**
     * As [save], after a failure: reads template [templateId] from the server again and sends only
     * what still differs, so a call that already went through is not sent twice. A field that is
     * not in [original] but has the name of a new field was created by an earlier try.
     */
    suspend fun retry(
        templateId: String,
        original: TemplateSnapshot?,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit = {}
    ): SaveTemplateResult
}

class SaveTemplateUseCaseImpl(private val templatesRepository: TemplatesRepository) : SaveTemplateUseCase {

    override suspend fun save(
        original: TemplateSnapshot?,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit
    ): SaveTemplateResult {
        duplicateName(edit)?.let { return SaveTemplateResult.DuplicateName(it) }
        if (original != null) return send(original.fields, original, edit, onProgress)

        val template = templatesRepository.createTemplate(edit.name)
            .getOrElse { return SaveTemplateResult.Failure(it, templateId = null) }
        // The template has no fields yet, so every field of the edit is new.
        val created = TemplateSnapshot(template, emptyList())
        return send(created.fields, created, edit, onProgress, extraDone = 1)
    }

    override suspend fun retry(
        templateId: String,
        original: TemplateSnapshot?,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit
    ): SaveTemplateResult {
        duplicateName(edit)?.let { return SaveTemplateResult.DuplicateName(it) }
        val current = runCatching {
            TemplateSnapshot(
                template = templatesRepository.getTemplate(templateId).getOrThrow(),
                fields = templatesRepository.getFields(templateId).getOrThrow()
            )
        }.getOrElse { return SaveTemplateResult.Failure(it, templateId) }
        return send(original?.fields.orEmpty(), current, edit, onProgress)
    }

    private fun duplicateName(edit: TemplateEdit): String? =
        edit.fields.groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.key

    /**
     * Takes [current] (the server now) to [edit]. [originalFields] are the fields the form loaded.
     * [extraDone] counts calls already done before this one (the create of the template).
     */
    private suspend fun send(
        originalFields: List<TemplateField>,
        current: TemplateSnapshot,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit,
        extraDone: Int = 0
    ): SaveTemplateResult {
        val templateId = current.template.id
        val calls = plan(originalFields, current, edit)
        var done = extraDone
        val total = calls.size + extraDone
        onProgress(SaveTemplateProgress(done, total))
        for (call in calls) {
            val result: Result<Any> = when (call) {
                is Call.Delete -> templatesRepository.deleteField(call.fieldId)

                is Call.Rename -> templatesRepository.renameTemplate(templateId, call.name)

                is Call.Patch -> templatesRepository.editField(
                    id = call.fieldId,
                    name = call.name,
                    position = call.position,
                    type = call.type,
                    choiceListId = call.choiceListId
                )

                is Call.Create -> templatesRepository.createField(
                    templateId = templateId,
                    name = call.field.name,
                    position = call.position,
                    type = call.field.type,
                    choiceListId = call.field.choiceListIdToSend()
                )
            }
            result.onFailure { return SaveTemplateResult.Failure(it, templateId) }
            done++
            onProgress(SaveTemplateProgress(done, total))
        }
        return SaveTemplateResult.Success(templateId)
    }

    private fun plan(originalFields: List<TemplateField>, current: TemplateSnapshot, edit: TemplateEdit): List<Call> {
        val originalIds = originalFields.mapTo(mutableSetOf()) { it.id }
        val keptIds = edit.fields.mapNotNullTo(mutableSetOf()) { it.fieldId }
        val currentById = current.fields.associateBy { it.id }
        // A field the form did not load, with the name of a new field: an earlier try created it.
        val created = current.fields.filter { it.id !in originalIds }.associateBy { it.name }

        val calls = mutableListOf<Call>()
        current.fields.filter { it.id in originalIds && it.id !in keptIds }.forEach { calls += Call.Delete(it.id) }
        if (edit.name != current.template.name) calls += Call.Rename(edit.name)

        val creates = mutableListOf<Call.Create>()
        edit.fields.forEachIndexed { index, field ->
            val position = index + 1
            val server =
                field.fieldId?.let(currentById::get) ?: if (field.fieldId == null) created[field.name] else null
            when {
                server != null -> patch(server, field, position)?.let { calls += it }

                // An existing field that is gone from the server (deleted on the web): not created again.
                field.fieldId != null -> Unit

                else -> creates += Call.Create(field, position)
            }
        }
        return calls + creates
    }

    /** One `PATCH` with what differs between [server] and [field], or null when nothing does. */
    private fun patch(server: TemplateField, field: EditedTemplateField, position: Int): Call.Patch? {
        val choiceListId = field.choiceListIdToSend()
        val call = Call.Patch(
            fieldId = server.id,
            name = field.name.takeIf { it != server.name },
            position = position.takeIf { it != server.position },
            type = field.type.takeIf { it != server.type },
            choiceListId = choiceListId.takeIf { it != server.choiceListId }
        )
        return call.takeIf { it.name != null || it.position != null || it.type != null || it.choiceListId != null }
    }

    private fun EditedTemplateField.choiceListIdToSend(): String? = choiceListId.takeIf { type == TYPE_CHOICE_LIST }

    private sealed interface Call {
        class Delete(val fieldId: String) : Call

        class Rename(val name: String) : Call

        class Patch(
            val fieldId: String,
            val name: String?,
            val position: Int?,
            val type: String?,
            val choiceListId: String?
        ) : Call

        class Create(val field: EditedTemplateField, val position: Int) : Call
    }

    private companion object {
        const val TYPE_CHOICE_LIST = "choice-list"
    }
}
