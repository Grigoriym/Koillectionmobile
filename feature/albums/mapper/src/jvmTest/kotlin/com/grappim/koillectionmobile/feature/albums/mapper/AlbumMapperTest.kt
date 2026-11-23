package com.grappim.koillectionmobile.feature.albums.mapper

import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.dto.AlbumDto
import com.grappim.koillectionmobile.feature.albums.dto.PhotoDto
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The test copy of the catalogue has no albums, so the values follow the `album:read` and `photo:read` groups. */
class AlbumMapperTest {

    private val mapper = AlbumMapper()
    private val serverUrl = "https://koi.example/sub/"

    @Test
    fun `toDomain takes the parent id from its IRI and resolves the image`() {
        val dto = AlbumDto(
            id = "child",
            title = "Summer",
            parent = "/api/albums/top",
            color = "A1B2C3",
            image = "uploads/u/a.jpg"
        )

        assertEquals(
            Album(
                id = "child",
                title = "Summer",
                parentId = "top",
                color = "A1B2C3",
                imageUrl = "https://koi.example/sub/uploads/u/a.jpg"
            ),
            mapper.toDomain(dto, serverUrl)
        )
    }

    @Test
    fun `toDomain maps every photo field and keeps the date part of takenAt`() {
        val dto = PhotoDto(
            id = "p1",
            title = "Beach",
            album = "/api/albums/child",
            comment = "Low tide",
            place = "Brest",
            image = "uploads/u/p.jpg",
            imageSmallThumbnail = "uploads/u/p_small.jpg",
            takenAt = "2026-07-14T23:30:00-11:00"
        )

        assertEquals(
            Photo(
                id = "p1",
                title = "Beach",
                albumId = "child",
                comment = "Low tide",
                place = "Brest",
                imageUrl = "https://koi.example/sub/uploads/u/p.jpg",
                thumbnailUrl = "https://koi.example/sub/uploads/u/p_small.jpg",
                takenAt = LocalDate(2026, 7, 14)
            ),
            mapper.toDomain(dto, serverUrl)
        )
    }

    @Test
    fun `toDomain keeps a missing takenAt null`() {
        assertNull(mapper.toDomain(PhotoDto(id = "p1", title = "Beach", album = "/api/albums/a"), serverUrl).takenAt)
    }
}
