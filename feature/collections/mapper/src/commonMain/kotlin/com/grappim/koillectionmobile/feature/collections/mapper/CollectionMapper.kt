package com.grappim.koillectionmobile.feature.collections.mapper

import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.dto.CollectionDto
import org.koin.core.annotation.Single

/**
 * [serverUrl] is the instance root with a trailing slash (`https://host/koillection/`). The server
 * sends the image path relative to it (`uploads/...`), and the mapper resolves it to a full URL,
 * as `ItemMapper` does.
 */
@Single
class CollectionMapper {

    fun toDomain(dto: CollectionDto, serverUrl: String): Collection = Collection(
        id = dto.id,
        title = dto.title,
        parentId = dto.parent?.substringAfterLast('/'),
        color = dto.color,
        imageUrl = dto.image?.let { serverUrl + it.removePrefix("/") },
        defaultTemplateId = dto.itemsDefaultTemplate?.substringAfterLast('/')
    )
}
