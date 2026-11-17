package com.grappim.koillectionmobile.feature.templates.domain

/**
 * Access to templates, their fields and choice lists. A failure is a `KoillectionError`
 * (`core:domain`) inside the [Result].
 */
interface TemplatesRepository {

    /** All templates of the user, from all pages. */
    suspend fun getTemplates(): Result<List<Template>>

    suspend fun getTemplate(id: String): Result<Template>

    /** Creates a template with no fields. A blank [name] fails with a validation error. */
    suspend fun createTemplate(name: String): Result<Template>

    suspend fun renameTemplate(id: String, name: String): Result<Template>

    suspend fun deleteTemplate(id: String): Result<Unit>

    /** All fields of template [templateId], from all pages, sorted by position. */
    suspend fun getFields(templateId: String): Result<List<TemplateField>>

    /**
     * Adds a field to template [templateId]. A `choice-list` [type] needs [choiceListId]; the
     * server rejects the field without it.
     */
    suspend fun createField(
        templateId: String,
        name: String,
        position: Int,
        type: String,
        choiceListId: String? = null
    ): Result<TemplateField>

    /**
     * Changes only the arguments that are not null; the server keeps the other values. A null
     * [choiceListId] does not clear the field's choice list.
     */
    suspend fun editField(
        id: String,
        name: String? = null,
        position: Int? = null,
        type: String? = null,
        choiceListId: String? = null
    ): Result<TemplateField>

    suspend fun deleteField(id: String): Result<Unit>

    /** All choice lists of the user, from all pages. */
    suspend fun getChoiceLists(): Result<List<ChoiceList>>

    /** The server rejects a duplicate in [choices]. */
    suspend fun createChoiceList(name: String, choices: List<String>): Result<ChoiceList>

    /** Sets the name and replaces the choices with [choices], in their order. */
    suspend fun editChoiceList(id: String, name: String, choices: List<String>): Result<ChoiceList>

    suspend fun deleteChoiceList(id: String): Result<Unit>
}
