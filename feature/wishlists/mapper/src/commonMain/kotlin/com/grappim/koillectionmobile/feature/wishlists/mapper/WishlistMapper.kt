package com.grappim.koillectionmobile.feature.wishlists.mapper

import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.dto.WishDto
import com.grappim.koillectionmobile.feature.wishlists.dto.WishlistDto
import org.koin.core.annotation.Single

/**
 * [serverUrl] is the instance root with a trailing slash (`https://host/koillection/`). The server
 * sends image paths relative to it (`uploads/...`), and the mapper resolves them to full URLs, as
 * `CollectionMapper` does.
 */
@Single
class WishlistMapper {

    fun toDomain(dto: WishlistDto, serverUrl: String): Wishlist = Wishlist(
        id = dto.id,
        name = dto.name,
        parentId = dto.parent?.substringAfterLast('/'),
        color = dto.color,
        imageUrl = dto.image?.let { resolve(it, serverUrl) }
    )

    fun toDomain(dto: WishDto, serverUrl: String): Wish = Wish(
        id = dto.id,
        name = dto.name,
        wishlistId = dto.wishlist.substringAfterLast('/'),
        url = dto.url,
        price = dto.price,
        currency = dto.currency,
        comment = dto.comment,
        imageUrl = dto.image?.let { resolve(it, serverUrl) },
        thumbnailUrl = dto.imageSmallThumbnail?.let { resolve(it, serverUrl) }
    )

    private fun resolve(path: String, serverUrl: String): String = serverUrl + path.removePrefix("/")
}
