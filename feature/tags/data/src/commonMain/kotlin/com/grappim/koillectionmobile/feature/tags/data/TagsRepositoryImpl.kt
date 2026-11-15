package com.grappim.koillectionmobile.feature.tags.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.tags.dto.ItemTagsPatchDto
import com.grappim.koillectionmobile.feature.tags.dto.TagCategoryDto
import com.grappim.koillectionmobile.feature.tags.dto.TagDto
import com.grappim.koillectionmobile.feature.tags.mapper.TagMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import org.koin.core.annotation.Single

/** No cache: every call is a round trip. Paths are relative to the stored server URL. */
@Single(binds = [TagsRepository::class])
internal class TagsRepositoryImpl(private val httpClient: HttpClient, private val mapper: TagMapper) :
    TagsRepository {

    override suspend fun getTags(): Result<List<Tag>> = resultOf {
        httpClient.getAllHydraPages<TagDto>("api/tags").map(mapper::toDomain)
    }

    override suspend fun getTagCategories(): Result<List<TagCategory>> = resultOf {
        httpClient.getAllHydraPages<TagCategoryDto>("api/tag_categories").map(mapper::toDomain)
    }

    override suspend fun getItemTags(itemId: String): Result<List<Tag>> = resultOf {
        httpClient.getAllHydraPages<TagDto>("api/items/$itemId/tags").map(mapper::toDomain)
    }

    /** The answer is the whole item; the tags feature has no use for it. */
    override suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit> = resultOf {
        httpClient.patch("api/items/$itemId") {
            contentType(ContentTypeMergePatchJson)
            setBody(ItemTagsPatchDto(tags = tagIds.map { "/api/tags/$it" }))
        }
        Unit
    }
}
