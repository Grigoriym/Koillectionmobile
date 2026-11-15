package com.grappim.koillectionmobile.feature.collections.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.collections.dto.CollectionDto
import com.grappim.koillectionmobile.feature.collections.mapper.CollectionMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.annotation.Single

/** No cache: every call is a round trip. Paths are relative to the stored server URL. */
@Single(binds = [CollectionsRepository::class])
internal class CollectionsRepositoryImpl(private val httpClient: HttpClient, private val mapper: CollectionMapper) :
    CollectionsRepository {

    override suspend fun getCollections(): Result<List<Collection>> = resultOf {
        httpClient.getAllHydraPages<CollectionDto>(COLLECTIONS_PATH).map(mapper::toDomain)
    }

    override suspend fun getCollection(id: String): Result<Collection> = resultOf {
        mapper.toDomain(httpClient.get("$COLLECTIONS_PATH/$id").body<CollectionDto>())
    }

    override suspend fun getChildren(id: String): Result<List<Collection>> = resultOf {
        httpClient.getAllHydraPages<CollectionDto>("$COLLECTIONS_PATH/$id/children").map(mapper::toDomain)
    }
}

private const val COLLECTIONS_PATH = "api/collections"
