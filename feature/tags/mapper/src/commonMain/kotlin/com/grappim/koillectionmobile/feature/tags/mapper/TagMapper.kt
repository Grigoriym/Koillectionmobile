package com.grappim.koillectionmobile.feature.tags.mapper

import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.dto.TagCategoryDto
import com.grappim.koillectionmobile.feature.tags.dto.TagDto
import org.koin.core.annotation.Single

@Single
class TagMapper {

    fun toDomain(dto: TagDto): Tag = Tag(
        id = dto.id,
        label = dto.label,
        description = dto.description,
        categoryId = dto.category?.substringAfterLast('/')
    )

    fun toDomain(dto: TagCategoryDto): TagCategory = TagCategory(
        id = dto.id,
        label = dto.label,
        description = dto.description,
        color = dto.color
    )
}
