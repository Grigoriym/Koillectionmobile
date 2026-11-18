package com.grappim.koillectionmobile.feature.templates.domain

/**
 * Keeps templates and fields in memory and records each write as a line in [calls]. A write whose
 * line starts with [failOn] fails once, before it changes anything.
 */
class RecordingTemplatesRepository : TemplatesRepository {
    val templates = mutableMapOf<String, Template>()
    val fields = mutableMapOf<String, Pair<String, TemplateField>>()
    val calls = mutableListOf<String>()
    var failOn: String? = null
    private var nextId = 1

    val error = IllegalStateException("server down")

    fun fieldsOf(templateId: String): List<TemplateField> =
        fields.values.filter { it.first == templateId }.map { it.second }.sortedBy { it.position }

    private fun write(call: String): Result<Unit> {
        calls += call
        val prefix = failOn
        if (prefix != null && call.startsWith(prefix)) {
            failOn = null
            return Result.failure(error)
        }
        return Result.success(Unit)
    }

    override suspend fun getTemplate(id: String): Result<Template> = Result.success(templates.getValue(id))

    override suspend fun getFields(templateId: String): Result<List<TemplateField>> =
        Result.success(fieldsOf(templateId))

    override suspend fun createTemplate(name: String): Result<Template> = write("createTemplate $name").map {
        Template(id = "t${nextId++}", name = name).also { templates[it.id] = it }
    }

    override suspend fun renameTemplate(id: String, name: String): Result<Template> =
        write("renameTemplate $id $name").map { Template(id, name).also { templates[id] = it } }

    override suspend fun createField(
        templateId: String,
        name: String,
        position: Int,
        type: String,
        choiceListId: String?
    ): Result<TemplateField> = write("createField $name $position $type $choiceListId").map {
        TemplateField("f${nextId++}", name, position, type, choiceListId).also { fields[it.id] = templateId to it }
    }

    override suspend fun editField(
        id: String,
        name: String?,
        position: Int?,
        type: String?,
        choiceListId: String?
    ): Result<TemplateField> = write("editField $id $name $position $type $choiceListId").map {
        val (templateId, old) = fields.getValue(id)
        old.copy(
            name = name ?: old.name,
            position = position ?: old.position,
            type = type ?: old.type,
            choiceListId = choiceListId ?: old.choiceListId
        ).also { fields[id] = templateId to it }
    }

    override suspend fun deleteField(id: String): Result<Unit> = write("deleteField $id").map {
        fields.remove(id)
    }.map {}

    override suspend fun getTemplates(): Result<List<Template>> = error("not used")

    override suspend fun deleteTemplate(id: String): Result<Unit> = error("not used")

    override suspend fun getChoiceLists(): Result<List<ChoiceList>> = error("not used")

    override suspend fun createChoiceList(name: String, choices: List<String>): Result<ChoiceList> = error("not used")

    override suspend fun editChoiceList(id: String, name: String, choices: List<String>): Result<ChoiceList> =
        error("not used")

    override suspend fun deleteChoiceList(id: String): Result<Unit> = error("not used")
}
