package com.grappim.koillectionmobile.feature.templates.mapper

import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.dto.ChoiceListDto
import com.grappim.koillectionmobile.feature.templates.dto.FieldDto
import com.grappim.koillectionmobile.feature.templates.dto.TemplateDto
import org.koin.core.annotation.Single

@Single
class TemplateMapper {

    fun toDomain(dto: TemplateDto): Template = Template(id = dto.id, name = dto.name)

    fun toDomain(dto: FieldDto): TemplateField = TemplateField(
        id = dto.id,
        name = dto.name,
        position = dto.position,
        type = dto.type,
        choiceListId = dto.choiceList?.substringAfterLast('/')
    )

    fun toDomain(dto: ChoiceListDto): ChoiceList = ChoiceList(id = dto.id, name = dto.name, choices = dto.choices)
}
