package com.grappim.koillectionmobile.feature.templates.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import com.grappim.koillectionmobile.feature.templates.dto.ChoiceListDto
import com.grappim.koillectionmobile.feature.templates.dto.ChoiceListWriteDto
import com.grappim.koillectionmobile.feature.templates.dto.FieldDto
import com.grappim.koillectionmobile.feature.templates.dto.FieldPatchDto
import com.grappim.koillectionmobile.feature.templates.dto.NewFieldDto
import com.grappim.koillectionmobile.feature.templates.dto.TemplateDto
import com.grappim.koillectionmobile.feature.templates.dto.TemplateNameDto
import com.grappim.koillectionmobile.feature.templates.mapper.TemplateMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. Paths are relative to the stored server URL. A request
 * body names a template or a choice list by IRI (`/api/templates/<id>`, `/api/choice_lists/<id>`).
 */
@Single(binds = [TemplatesRepository::class])
internal class TemplatesRepositoryImpl(private val httpClient: HttpClient, private val mapper: TemplateMapper) :
    TemplatesRepository {

    override suspend fun getTemplates(): Result<List<Template>> = resultOf {
        httpClient.getAllHydraPages<TemplateDto>(TEMPLATES_PATH).map(mapper::toDomain)
    }

    override suspend fun getTemplate(id: String): Result<Template> = resultOf {
        mapper.toDomain(httpClient.get("$TEMPLATES_PATH/$id").body<TemplateDto>())
    }

    override suspend fun createTemplate(name: String): Result<Template> = resultOf {
        val dto = httpClient.post(TEMPLATES_PATH) {
            contentType(ContentTypeLdJson)
            setBody(TemplateNameDto(name = name))
        }.body<TemplateDto>()
        mapper.toDomain(dto)
    }

    override suspend fun renameTemplate(id: String, name: String): Result<Template> = resultOf {
        val dto = httpClient.patch("$TEMPLATES_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(TemplateNameDto(name = name))
        }.body<TemplateDto>()
        mapper.toDomain(dto)
    }

    override suspend fun deleteTemplate(id: String): Result<Unit> = delete("$TEMPLATES_PATH/$id")

    /** The server orders a template's fields by position too (`Template::$fields`); the sort does not rely on it. */
    override suspend fun getFields(templateId: String): Result<List<TemplateField>> = resultOf {
        httpClient.getAllHydraPages<FieldDto>("$TEMPLATES_PATH/$templateId/fields")
            .map(mapper::toDomain)
            .sortedBy { it.position }
    }

    override suspend fun createField(
        templateId: String,
        name: String,
        position: Int,
        type: String,
        choiceListId: String?
    ): Result<TemplateField> = resultOf {
        val dto = httpClient.post(FIELDS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(
                NewFieldDto(
                    name = name,
                    position = position,
                    type = type,
                    template = "/$TEMPLATES_PATH/$templateId",
                    choiceList = choiceListId?.let(::choiceListIri)
                )
            )
        }.body<FieldDto>()
        mapper.toDomain(dto)
    }

    override suspend fun editField(
        id: String,
        name: String?,
        position: Int?,
        type: String?,
        choiceListId: String?
    ): Result<TemplateField> = resultOf {
        val dto = httpClient.patch("$FIELDS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(
                FieldPatchDto(
                    name = name,
                    position = position,
                    type = type,
                    choiceList = choiceListId?.let(::choiceListIri)
                )
            )
        }.body<FieldDto>()
        mapper.toDomain(dto)
    }

    override suspend fun deleteField(id: String): Result<Unit> = delete("$FIELDS_PATH/$id")

    override suspend fun getChoiceLists(): Result<List<ChoiceList>> = resultOf {
        httpClient.getAllHydraPages<ChoiceListDto>(CHOICE_LISTS_PATH).map(mapper::toDomain)
    }

    override suspend fun createChoiceList(name: String, choices: List<String>): Result<ChoiceList> = resultOf {
        val dto = httpClient.post(CHOICE_LISTS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(ChoiceListWriteDto(name = name, choices = choices))
        }.body<ChoiceListDto>()
        mapper.toDomain(dto)
    }

    override suspend fun editChoiceList(id: String, name: String, choices: List<String>): Result<ChoiceList> =
        resultOf {
            val dto = httpClient.patch("$CHOICE_LISTS_PATH/$id") {
                contentType(ContentTypeMergePatchJson)
                setBody(ChoiceListWriteDto(name = name, choices = choices))
            }.body<ChoiceListDto>()
            mapper.toDomain(dto)
        }

    override suspend fun deleteChoiceList(id: String): Result<Unit> = delete("$CHOICE_LISTS_PATH/$id")

    private suspend fun delete(path: String): Result<Unit> = resultOf {
        httpClient.delete(path)
        Unit
    }
}

private fun choiceListIri(id: String): String = "/$CHOICE_LISTS_PATH/$id"

private const val TEMPLATES_PATH = "api/templates"
private const val FIELDS_PATH = "api/fields"
private const val CHOICE_LISTS_PATH = "api/choice_lists"
