package com.grappim.koillectionmobile.feature.collections.mapper

import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.dto.CollectionDto
import org.koin.core.annotation.Single

@Single
class CollectionMapper {

    fun toDomain(dto: CollectionDto): Collection = Collection(
        id = dto.id,
        title = dto.title,
        parentId = dto.parent?.substringAfterLast('/'),
        color = dto.color,
        imagePath = dto.image
    )
}
