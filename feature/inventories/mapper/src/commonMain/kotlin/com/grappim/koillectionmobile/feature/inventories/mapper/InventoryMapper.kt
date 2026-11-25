package com.grappim.koillectionmobile.feature.inventories.mapper

import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryCollection
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryItem
import com.grappim.koillectionmobile.feature.inventories.dto.InventoryCollectionDto
import com.grappim.koillectionmobile.feature.inventories.dto.InventoryDto
import com.grappim.koillectionmobile.feature.inventories.dto.InventoryItemDto
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Single

/**
 * The server sends `createdAt` in its own time zone, with the offset
 * (`2026-11-25T10:19:26+01:00`, M12.1). The app keeps the date part as the server wrote it, with
 * no time zone conversion, as `LoanMapper` does.
 */
@Single
class InventoryMapper {

    fun toDomain(dto: InventoryDto): Inventory = Inventory(
        id = dto.id,
        name = dto.name,
        createdAt = LocalDate.parse(dto.createdAt.substringBefore('T')),
        collections = dto.content.map(::toDomain)
    )

    private fun toDomain(dto: InventoryCollectionDto): InventoryCollection = InventoryCollection(
        id = dto.id,
        title = dto.title,
        children = dto.children.map(::toDomain),
        items = dto.items.map(::toDomain),
        totalItems = dto.totalItems,
        checkedItems = dto.totalCheckedItems
    )

    private fun toDomain(dto: InventoryItemDto): InventoryItem =
        InventoryItem(id = dto.id, name = dto.name, checked = dto.checked)
}
