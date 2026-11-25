package com.grappim.koillectionmobile.feature.logs.mapper

import com.grappim.koillectionmobile.feature.logs.domain.LogAction
import com.grappim.koillectionmobile.feature.logs.domain.LogEntry
import com.grappim.koillectionmobile.feature.logs.domain.LogObjectType
import com.grappim.koillectionmobile.feature.logs.dto.LogDto
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.parse
import org.koin.core.annotation.Single

/**
 * The server sends `loggedAt` in its own time zone, with the offset. The app keeps the date and
 * time as the server wrote them, with no time zone conversion, as `InventoryMapper` does.
 */
@Single
class LogMapper {

    fun toDomain(dto: LogDto): LogEntry = LogEntry(
        id = dto.id,
        action = when (dto.logType) {
            "create" -> LogAction.Create
            "delete" -> LogAction.Delete
            else -> LogAction.Unknown
        },
        objectType = OBJECT_TYPES[dto.objectClass.substringAfterLast('\\')] ?: LogObjectType.Unknown,
        objectLabel = dto.objectLabel,
        loggedAt = DateTimeComponents.parse(dto.loggedAt, DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
            .toLocalDateTime()
    )
}

/** Short PHP class name → type. */
private val OBJECT_TYPES: Map<String, LogObjectType> = mapOf(
    "Collection" to LogObjectType.Collection,
    "Item" to LogObjectType.Item,
    "Template" to LogObjectType.Template,
    "ChoiceList" to LogObjectType.ChoiceList,
    "Inventory" to LogObjectType.Inventory,
    "Tag" to LogObjectType.Tag,
    "TagCategory" to LogObjectType.TagCategory,
    "Album" to LogObjectType.Album,
    "Photo" to LogObjectType.Photo,
    "Wishlist" to LogObjectType.Wishlist,
    "Wish" to LogObjectType.Wish
)
