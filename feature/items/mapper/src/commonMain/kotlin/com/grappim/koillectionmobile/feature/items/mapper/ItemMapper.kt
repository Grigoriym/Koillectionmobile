package com.grappim.koillectionmobile.feature.items.mapper

import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.dto.DatumDto
import com.grappim.koillectionmobile.feature.items.dto.ItemDto
import org.koin.core.annotation.Single

/**
 * [serverUrl] is the instance root with a trailing slash (`https://host/koillection/`). The
 * server sends image paths relative to it (`uploads/...`), and the mapper resolves them to full
 * URLs.
 */
@Single
class ItemMapper {

    fun toDomain(dto: ItemDto, serverUrl: String): Item = Item(
        id = dto.id,
        name = dto.name,
        quantity = dto.quantity,
        collectionId = dto.collection?.substringAfterLast('/'),
        image = image(dto.image, dto.imageSmallThumbnail, dto.imageLargeThumbnail, serverUrl)
    )

    fun toDomain(dto: DatumDto, serverUrl: String): Datum = Datum(
        id = dto.id,
        type = dto.type,
        label = dto.label,
        value = dto.value,
        position = dto.position,
        image = image(dto.image, dto.imageSmallThumbnail, dto.imageLargeThumbnail, serverUrl)
    )

    private fun image(path: String?, small: String?, large: String?, serverUrl: String): Image? = path?.let {
        Image(
            url = resolve(it, serverUrl),
            smallThumbnailUrl = small?.let { thumbnail -> resolve(thumbnail, serverUrl) },
            largeThumbnailUrl = large?.let { thumbnail -> resolve(thumbnail, serverUrl) }
        )
    }

    private fun resolve(path: String, serverUrl: String): String = serverUrl + path.removePrefix("/")
}
