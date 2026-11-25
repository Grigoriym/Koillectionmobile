package com.grappim.koillectionmobile.feature.logs.mapper

import com.grappim.koillectionmobile.feature.logs.domain.LogAction
import com.grappim.koillectionmobile.feature.logs.domain.LogEntry
import com.grappim.koillectionmobile.feature.logs.domain.LogObjectType
import com.grappim.koillectionmobile.feature.logs.dto.LogDto
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Bodies are the M13.1 fixtures from the 1.8.5 local test instance. */
class LogMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = LogMapper()

    private fun fixture(name: String): String = File("../../../fixtures/koillection-1.8.5/$name").readText()

    private fun page(name: String): List<LogEntry> =
        json.parseToJsonElement(fixture(name)).jsonObject.getValue("member").jsonArray
            .map { mapper.toDomain(json.decodeFromJsonElement<LogDto>(it)) }

    @Test
    fun `one log maps every field`() {
        val dto = json.decodeFromString<LogDto>(fixture("log_one.json"))

        assertEquals(
            LogEntry(
                id = "01a2122a-2671-7e58-b2f5-1518ee70917e",
                action = LogAction.Create,
                objectType = LogObjectType.Photo,
                objectLabel = "Comic con stall",
                loggedAt = LocalDateTime(2026, 11, 25, 11, 4, 41)
            ),
            mapper.toDomain(dto)
        )
    }

    /** The server sends the oldest first; the mapper keeps the order (the repository reverses). */
    @Test
    fun `a page maps in the server's ascending order`() {
        val logs = page("logs_page_middle.json")

        assertEquals(30, logs.size)
        assertEquals(logs.sortedBy { it.id }, logs)
        assertEquals(logs.sortedBy { it.loggedAt }, logs)
    }

    /** The three pages hold 10 of the 11 classes (no `TagCategory`). */
    @Test
    fun `no row of the fixtures maps to Unknown`() {
        val logs = listOf("logs_page_first.json", "logs_page_middle.json", "logs_page_last.json").flatMap(::page)

        assertEquals(setOf(LogAction.Create, LogAction.Delete), logs.map { it.action }.toSet())
        assertEquals(emptyList(), logs.filter { it.objectType == LogObjectType.Unknown })
    }

    /** `HistoryFilterEnum::FILTERS_CLASS`, as the PHP class names of `App\Entity`. */
    @Test
    fun `each of the 11 classes maps to its own type`() {
        val names = listOf(
            "Collection", "Item", "Template", "ChoiceList", "Inventory", "Tag",
            "TagCategory", "Album", "Photo", "Wishlist", "Wish"
        )

        assertEquals(
            LogObjectType.entries - LogObjectType.Unknown,
            names.map { mapper.toDomain(dto(objectClass = "App\\Entity\\$it")).objectType }
        )
    }

    @Test
    fun `a delete maps to Delete with its class`() {
        val first = page("logs_page_middle.json").first()

        assertEquals(LogAction.Delete, first.action)
        assertEquals(LogObjectType.Wish, first.objectType)
        assertEquals("Les Voyageurs, tome 3", first.objectLabel)
    }

    @Test
    fun `an unknown type and an unknown class map to Unknown`() {
        val log = mapper.toDomain(dto(logType = "update", objectClass = "App\\Entity\\Field"))

        assertEquals(LogAction.Unknown, log.action)
        assertEquals(LogObjectType.Unknown, log.objectType)
    }

    @Test
    fun `a missing type maps to Unknown`() {
        val log = mapper.toDomain(
            json.decodeFromString<LogDto>(
                """{"id":"l","loggedAt":"2026-11-25T11:04:41+01:00","objectLabel":"L","objectClass":"App\\Entity\\Tag"}"""
            )
        )

        assertEquals(LogAction.Unknown, log.action)
        assertEquals(LogObjectType.Tag, log.objectType)
    }

    /** A late time with a far offset must not move the date: the mapper does no time zone conversion. */
    @Test
    fun `loggedAt keeps the server's date and time at a far offset`() {
        assertEquals(
            LocalDateTime(2026, 11, 25, 23, 30, 5),
            mapper.toDomain(dto(loggedAt = "2026-11-25T23:30:05-11:00")).loggedAt
        )
    }

    private fun dto(
        logType: String? = "create",
        objectClass: String = "App\\Entity\\Item",
        loggedAt: String = "2026-11-25T11:04:41+01:00"
    ) = LogDto(id = "l", logType = logType, loggedAt = loggedAt, objectLabel = "L", objectClass = objectClass)
}
