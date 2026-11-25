package com.grappim.koillectionmobile.feature.inventories.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.feature.inventories.domain.InventoriesRepository
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.dto.InventoryDto
import com.grappim.koillectionmobile.feature.inventories.mapper.InventoryMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import org.koin.core.annotation.Single

/** No cache: every call is a round trip. Paths are relative to the stored server URL. */
@Single(binds = [InventoriesRepository::class])
internal class InventoriesRepositoryImpl(private val httpClient: HttpClient, private val mapper: InventoryMapper) :
    InventoriesRepository {

    override suspend fun getInventories(): Result<List<Inventory>> = resultOf {
        httpClient.getAllHydraPages<InventoryDto>(INVENTORIES_PATH).map(mapper::toDomain)
    }

    override suspend fun getInventory(id: String): Result<Inventory> = resultOf {
        mapper.toDomain(httpClient.get("$INVENTORIES_PATH/$id").body<InventoryDto>())
    }

    override suspend fun deleteInventory(id: String): Result<Unit> = resultOf {
        httpClient.delete("$INVENTORIES_PATH/$id")
        Unit
    }
}

private const val INVENTORIES_PATH = "api/inventories"
