package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import kotlinx.coroutines.CompletableDeferred

/** Each read takes the first queued response of its kind. Queue one before the call. */
class FakeTemplatesRepository : TemplatesRepository {
    val templates = ArrayDeque<CompletableDeferred<Result<List<Template>>>>()
    val fields = ArrayDeque<CompletableDeferred<Result<List<TemplateField>>>>()
    val choiceLists = ArrayDeque<CompletableDeferred<Result<List<ChoiceList>>>>()
    val template = ArrayDeque<CompletableDeferred<Result<Template>>>()
    val deletes = ArrayDeque<CompletableDeferred<Result<Unit>>>()
    val choiceListWrites = ArrayDeque<CompletableDeferred<Result<ChoiceList>>>()
    val choiceListDeletes = ArrayDeque<CompletableDeferred<Result<Unit>>>()

    /** The choice list writes, in order: `create:<name>:<choices>` or `edit:<id>:<name>:<choices>`. */
    val choiceListWriteCalls = mutableListOf<String>()
    val choiceListDeleteCalls = mutableListOf<String>()
    var templateCalls = 0
    val getTemplateCalls = mutableListOf<String>()
    val deleteCalls = mutableListOf<String>()
    val fieldCalls = mutableListOf<String>()
    var choiceListCalls = 0

    override suspend fun getTemplates(): Result<List<Template>> {
        templateCalls++
        return templates.first().await()
    }

    override suspend fun getFields(templateId: String): Result<List<TemplateField>> {
        fieldCalls += templateId
        return fields.first().await()
    }

    override suspend fun getChoiceLists(): Result<List<ChoiceList>> {
        choiceListCalls++
        return choiceLists.first().await()
    }

    override suspend fun getTemplate(id: String): Result<Template> {
        getTemplateCalls += id
        return template.first().await()
    }

    override suspend fun createTemplate(name: String): Result<Template> = error("not used")

    override suspend fun renameTemplate(id: String, name: String): Result<Template> = error("not used")

    override suspend fun deleteTemplate(id: String): Result<Unit> {
        deleteCalls += id
        return deletes.first().await()
    }

    override suspend fun createField(
        templateId: String,
        name: String,
        position: Int,
        type: String,
        choiceListId: String?
    ): Result<TemplateField> = error("not used")

    override suspend fun editField(
        id: String,
        name: String?,
        position: Int?,
        type: String?,
        choiceListId: String?
    ): Result<TemplateField> = error("not used")

    override suspend fun deleteField(id: String): Result<Unit> = error("not used")

    override suspend fun createChoiceList(name: String, choices: List<String>): Result<ChoiceList> {
        choiceListWriteCalls += "create:$name:$choices"
        return choiceListWrites.first().await()
    }

    override suspend fun editChoiceList(id: String, name: String, choices: List<String>): Result<ChoiceList> {
        choiceListWriteCalls += "edit:$id:$name:$choices"
        return choiceListWrites.first().await()
    }

    override suspend fun deleteChoiceList(id: String): Result<Unit> {
        choiceListDeleteCalls += id
        return choiceListDeletes.first().await()
    }
}
