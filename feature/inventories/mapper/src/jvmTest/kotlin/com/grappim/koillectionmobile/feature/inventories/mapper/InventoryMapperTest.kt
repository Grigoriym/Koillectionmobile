package com.grappim.koillectionmobile.feature.inventories.mapper

import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryCollection
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryItem
import com.grappim.koillectionmobile.feature.inventories.dto.InventoryDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Bodies are the M12.1 fixtures from the 1.8.5 local test instance (`fixtures/seed.py`). */
class InventoryMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = InventoryMapper()

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    /**
     * "Comics" is a parent with no items of its own; its two children hold the items. Three ticks
     * stored four (plan §2), so "Les Voyageurs, tome 2" is the one unchecked item.
     */
    @Test
    fun `one inventory maps the whole collection tree with counts and checked flags`() {
        val dto = json.decodeFromString<InventoryDto>(fixture("inventory_one.json"))

        assertEquals(comicsInventory, mapper.toDomain(dto))
    }

    @Test
    fun `each member of the list page maps with its own tree`() {
        val members = json.parseToJsonElement(fixture("inventories_page.json")).jsonObject.getValue("member").jsonArray
        val inventories = members.map { mapper.toDomain(json.decodeFromJsonElement<InventoryDto>(it)) }

        assertEquals(listOf(comicsInventory, boardGamesInventory), inventories)
    }

    /** `InventoryHandler::buildCollection` adds `children` to a parent node only through an append. */
    @Test
    fun `a node without children or items keys maps to empty lists`() {
        val dto = json.decodeFromString<InventoryDto>(
            """{"id":"inv","name":"N","createdAt":"2026-11-25T10:19:26+01:00",""" +
                """"content":[{"id":"c","title":"C","totalItems":0,"totalCheckedItems":0}]}"""
        )

        assertEquals(
            listOf(InventoryCollection("c", "C", emptyList(), emptyList(), totalItems = 0, checkedItems = 0)),
            mapper.toDomain(dto).collections
        )
    }

    @Test
    fun `a missing content key maps to no collections`() {
        val dto = json.decodeFromString<InventoryDto>(
            """{"id":"inv","name":"N","createdAt":"2026-11-25T10:19:26+01:00"}"""
        )

        assertEquals(emptyList(), mapper.toDomain(dto).collections)
    }

    /** A late time with a far offset must not move the date: the mapper does no time zone conversion. */
    @Test
    fun `createdAt keeps the server's date at a late time and a far offset`() {
        val dto = InventoryDto(id = "inv", name = "N", createdAt = "2026-11-25T23:30:00-11:00")

        assertEquals(LocalDate(2026, 11, 25), mapper.toDomain(dto).createdAt)
    }

    private val comicsInventory = Inventory(
        id = "01a21200-b8e9-70a7-86c3-017c367f3432",
        name = "Comics shelf check",
        createdAt = LocalDate(2026, 11, 25),
        collections = listOf(
            InventoryCollection(
                id = "01a21200-af47-7623-af1b-62aa9f1f73da",
                title = "Comics",
                children = listOf(
                    InventoryCollection(
                        id = "01a21200-afcc-7e4e-81a3-ff308e607ee6",
                        title = "Franco-Belgian",
                        children = emptyList(),
                        items = listOf(
                            InventoryItem(
                                "01a21200-b486-7aaa-9a85-499e1ff2d42d",
                                "Les Voyageurs, tome 2",
                                checked = false
                            ),
                            InventoryItem(
                                "01a21200-b366-7269-826e-d0bbbdae9863",
                                "Les Voyageurs, tome 1",
                                checked = true
                            )
                        ),
                        totalItems = 2,
                        checkedItems = 1
                    ),
                    InventoryCollection(
                        id = "01a21200-af9b-7dd3-860a-a6728b414bd3",
                        title = "Superheroes",
                        children = emptyList(),
                        items = listOf(
                            InventoryItem("01a21200-affe-7d47-bce5-db1118f86406", "Night Watch #1", checked = true),
                            InventoryItem("01a21200-b127-7596-93db-22e6d3b7fa78", "Night Watch #2", checked = true),
                            InventoryItem(
                                "01a21200-b247-795c-a336-801b9bc50ae2",
                                "Night Watch: Collected",
                                checked = true
                            )
                        ),
                        totalItems = 3,
                        checkedItems = 3
                    )
                ),
                items = emptyList(),
                totalItems = 5,
                checkedItems = 4
            )
        )
    )

    private val boardGamesInventory = Inventory(
        id = "01a21200-b92f-7254-9492-d05d83bb3ebe",
        name = "Board games check",
        createdAt = LocalDate(2026, 11, 25),
        collections = listOf(
            InventoryCollection(
                id = "01a21200-b5a6-7af6-83a7-8fbb4c872194",
                title = "Board games",
                children = emptyList(),
                items = listOf(
                    InventoryItem("01a21200-b6e5-7e11-a63b-c7e8445ac931", "Star Cartographers", checked = false),
                    InventoryItem("01a21200-b65e-7038-9b2a-5e284f3208f0", "Tower Siege", checked = false),
                    InventoryItem("01a21200-b5d4-7018-898f-7b36706da963", "River Traders", checked = false)
                ),
                totalItems = 3,
                checkedItems = 0
            )
        )
    )
}
