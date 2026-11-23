package com.grappim.koillectionmobile.feature.albums.mapper

import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.dto.AlbumDto
import com.grappim.koillectionmobile.feature.albums.dto.PhotoDto
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/**
 * [serverUrl] is the instance root with a trailing slash (`https://host/koillection/`). The server
 * sends image paths relative to it (`uploads/...`), and the mapper resolves them to full URLs, as
 * `WishlistMapper` does.
 *
 * `takenAt` keeps the date part as the server wrote it, with no time zone conversion, as
 * `LoanMapper` does.
 */
@Single
class AlbumMapper {

    fun toDomain(dto: AlbumDto, serverUrl: String): Album = Album(
        id = dto.id,
        title = dto.title,
        parentId = dto.parent?.substringAfterLast('/'),
        color = dto.color,
        imageUrl = dto.image?.let { resolve(it, serverUrl) }
    )

    fun toDomain(dto: PhotoDto, serverUrl: String): Photo = Photo(
        id = dto.id,
        title = dto.title,
        albumId = dto.album.substringAfterLast('/'),
        comment = dto.comment,
        place = dto.place,
        imageUrl = dto.image?.let { resolve(it, serverUrl) },
        thumbnailUrl = dto.imageSmallThumbnail?.let { resolve(it, serverUrl) },
        takenAt = dto.takenAt?.let { LocalDate.parse(it.substringBefore('T')) }
    )

    private fun resolve(path: String, serverUrl: String): String = serverUrl + path.removePrefix("/")
}
