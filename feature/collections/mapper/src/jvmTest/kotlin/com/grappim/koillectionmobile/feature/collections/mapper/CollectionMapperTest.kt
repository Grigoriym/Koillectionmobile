package com.grappim.koillectionmobile.feature.collections.mapper

import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.dto.CollectionDto
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Bodies are the M1.1 fixtures from the 1.8.5 local test instance. */
class CollectionMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = CollectionMapper()

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    @Test
    fun `a top-level collection from the server maps every field`() {
        val dto = json.decodeFromString<CollectionDto>(fixture("collection_one.json"))

        assertEquals(
            Collection(
                id = "01a1de6f-bfad-78b6-a916-7266d9e3dfff",
                title = "Books",
                parentId = null,
                color = "EEEEEE",
                imageUrl = null
            ),
            mapper.toDomain(dto, SERVER_URL)
        )
    }

    @Test
    fun `the parent IRI maps to the parent's id, with a server subpath too`() {
        val dto = CollectionDto(
            id = "child",
            title = "Novels",
            parent = "/koillection/api/collections/01a1de6f-bfad-78b6-a916-7266d9e3dfff"
        )

        val collection = mapper.toDomain(dto, SERVER_URL)

        assertEquals("01a1de6f-bfad-78b6-a916-7266d9e3dfff", collection.parentId)
    }

    @Test
    fun `the image path resolves to a full URL under the server root`() {
        val dto = CollectionDto(id = "a", title = "A", image = "uploads/user/cover.jpg")

        assertEquals("https://koi.example/sub/uploads/user/cover.jpg", mapper.toDomain(dto, SERVER_URL).imageUrl)
    }

    @Test
    fun `a leading slash on the image path does not double the slash`() {
        val dto = CollectionDto(id = "a", title = "A", image = "/uploads/user/cover.jpg")

        assertEquals("https://koi.example/sub/uploads/user/cover.jpg", mapper.toDomain(dto, SERVER_URL).imageUrl)
    }

    private companion object {
        const val SERVER_URL = "https://koi.example/sub/"
    }
}
