package com.grappim.koillectionmobile.feature.items.mapper

import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Image
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.dto.DatumDto
import com.grappim.koillectionmobile.feature.items.dto.ItemDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Bodies are the M1.1 fixtures from the 1.8.5 local test instance. */
class ItemMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = ItemMapper()

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    @Test
    fun `an item from the server maps every field, and its image paths become full URLs`() {
        val dto = json.decodeFromString<ItemDto>(fixture("item_one.json"))

        assertEquals(
            Item(
                id = "01a1de6f-bff4-7eb7-8d9f-973b4ac86da8",
                name = "Auswahl Deutscher Gedichte für höhere Schulen",
                quantity = 1,
                collectionId = "01a1de6f-bfad-78b6-a916-7266d9e3dfff",
                image = Image(
                    url = "$SERVER$UPLOADS/Cq7YNpThMJzPpVrKinW5.jpg",
                    smallThumbnailUrl = "$SERVER$UPLOADS/Cq7YNpThMJzPpVrKinW5_small.jpg",
                    largeThumbnailUrl = "$SERVER$UPLOADS/Cq7YNpThMJzPpVrKinW5_large.jpg"
                )
            ),
            mapper.toDomain(dto, SERVER)
        )
    }

    @Test
    fun `an item without an image has no image`() {
        val item = mapper.toDomain(ItemDto(id = "a", name = "A"), SERVER)

        assertNull(item.image)
        assertNull(item.collectionId)
    }

    @Test
    fun `a text field and an image field from the server`() {
        val data = json.parseToJsonElement(fixture("item_data.json")).jsonObject.getValue("member").jsonArray
            .map { json.decodeFromJsonElement<DatumDto>(it) }

        assertEquals(
            Datum(
                id = "01a1de6f-c0df-7aeb-9031-36f7f7957a7d",
                type = "text",
                label = "Author",
                value = "Theodor Echtermeyer",
                position = 1,
                image = null
            ),
            mapper.toDomain(data.first(), SERVER)
        )
        assertEquals(
            Datum(
                id = "01a1de6f-c126-7a23-aa77-830123493fc7",
                type = "image",
                label = "Title page",
                value = null,
                position = 12,
                image = Image(
                    url = "$SERVER$UPLOADS/1U8K7mE6pFHNZQqZjjLT.jpg",
                    smallThumbnailUrl = "$SERVER$UPLOADS/1U8K7mE6pFHNZQqZjjLT_small.jpg",
                    largeThumbnailUrl = "$SERVER$UPLOADS/1U8K7mE6pFHNZQqZjjLT_large.jpg"
                )
            ),
            mapper.toDomain(data.single { it.label == "Title page" }, SERVER)
        )
    }

    @Test
    fun `an image without thumbnails, and a path with a leading slash, still resolve once`() {
        val datum = mapper.toDomain(DatumDto(id = "d", type = "image", image = "/uploads/u/x.jpg"), SERVER)

        assertEquals(
            Image(url = "${SERVER}uploads/u/x.jpg", smallThumbnailUrl = null, largeThumbnailUrl = null),
            datum.image
        )
    }

    @Test
    fun `a choice-list field keeps the id of its choice list`() {
        val dto = DatumDto(
            id = "d",
            type = "choice-list",
            value = """["Ebook"]""",
            choiceList = "/api/choice_lists/list-1"
        )

        assertEquals("list-1", mapper.toDomain(dto, SERVER).choiceListId)
    }

    private companion object {
        /** With a subpath: the image URL must keep it. */
        const val SERVER = "https://koi.example/sub/"
        const val UPLOADS = "uploads/01a1de6f-03c6-7568-a2b7-9d7a173d6776"
    }
}
