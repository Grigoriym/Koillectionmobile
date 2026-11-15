package com.grappim.koillectionmobile.feature.tags.mapper

import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.dto.TagCategoryDto
import com.grappim.koillectionmobile.feature.tags.dto.TagDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Bodies are the M1.1 fixtures from the 1.8.5 local test instance. */
class TagMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = TagMapper()

    private inline fun <reified T> members(name: String): List<T> =
        json.parseToJsonElement(File("../../../fixtures/koillection-1.8.5/$name").readText())
            .jsonObject.getValue("member").jsonArray
            .map { json.decodeFromJsonElement<T>(it) }

    @Test
    fun `a tag from the server maps every field, and its category IRI becomes an ID`() {
        val dto = members<TagDto>("item_tags.json").first()

        assertEquals(
            Tag(
                id = "01a1de6f-bf62-7f41-800a-2cec7f1399ad",
                label = "Poetry",
                description = null,
                categoryId = "01a1de6f-beeb-7dc6-9fb6-265318d4090d"
            ),
            mapper.toDomain(dto)
        )
    }

    @Test
    fun `a tag without a category has no category ID`() {
        assertEquals(
            Tag(id = "t", label = "T", description = "d", categoryId = null),
            mapper.toDomain(TagDto(id = "t", label = "T", description = "d"))
        )
    }

    @Test
    fun `a tag category from the server maps every field`() {
        val dto = members<TagCategoryDto>("tag_categories.json").first()

        assertEquals(
            TagCategory(
                id = "01a1de6f-bee0-70ab-a6c9-079d04c2dcea",
                label = "Subject",
                description = null,
                color = "F3E5F5"
            ),
            mapper.toDomain(dto)
        )
    }
}
