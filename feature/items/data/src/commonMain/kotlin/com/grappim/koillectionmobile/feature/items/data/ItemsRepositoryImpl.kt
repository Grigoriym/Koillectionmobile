package com.grappim.koillectionmobile.feature.items.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.core.api.normalizeBaseUrl
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.items.dto.DatumDto
import com.grappim.koillectionmobile.feature.items.dto.ItemDto
import com.grappim.koillectionmobile.feature.items.mapper.ItemMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. Paths are relative to the stored server URL, and image
 * paths resolve against the same URL.
 */
@Single(binds = [ItemsRepository::class])
internal class ItemsRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrlProvider: BaseUrlProvider,
    private val mapper: ItemMapper
) : ItemsRepository {

    override suspend fun getItems(collectionId: String): Result<List<Item>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<ItemDto>("api/collections/$collectionId/items")
            .map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getItem(id: String): Result<Item> = resultOf {
        mapper.toDomain(httpClient.get("$ITEMS_PATH/$id").body<ItemDto>(), serverUrl())
    }

    /**
     * The server does not promise `position` order: `Item::$data`'s `OrderBy` is a Doctrine
     * collection setting, and API Platform reads `/items/{id}/data` with its own query.
     */
    override suspend fun getData(itemId: String): Result<List<Datum>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<DatumDto>("$ITEMS_PATH/$itemId/data")
            .map { mapper.toDomain(it, serverUrl) }
            .sortedWith(compareBy(nullsLast()) { it.position })
    }

    private fun serverUrl(): String = normalizeBaseUrl(baseUrlProvider.getBaseUrl())
}

private const val ITEMS_PATH = "api/items"
