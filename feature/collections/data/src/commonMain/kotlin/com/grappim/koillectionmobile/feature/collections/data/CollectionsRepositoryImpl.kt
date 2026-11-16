package com.grappim.koillectionmobile.feature.collections.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.core.api.normalizeBaseUrl
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.collections.dto.CollectionDto
import com.grappim.koillectionmobile.feature.collections.dto.CollectionPatchDto
import com.grappim.koillectionmobile.feature.collections.dto.NewCollectionDto
import com.grappim.koillectionmobile.feature.collections.mapper.CollectionMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.escapeIfNeeded
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. Paths are relative to the stored server URL, and image
 * paths resolve against the same URL. A request body names other collections by IRI
 * (`/api/collections/<id>`).
 */
@Single(binds = [CollectionsRepository::class])
internal class CollectionsRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrlProvider: BaseUrlProvider,
    private val mapper: CollectionMapper
) : CollectionsRepository {

    override suspend fun getCollections(): Result<List<Collection>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<CollectionDto>(COLLECTIONS_PATH).map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getCollection(id: String): Result<Collection> = resultOf {
        mapper.toDomain(httpClient.get("$COLLECTIONS_PATH/$id").body<CollectionDto>(), serverUrl())
    }

    override suspend fun getChildren(id: String): Result<List<Collection>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<CollectionDto>("$COLLECTIONS_PATH/$id/children")
            .map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun createCollection(title: String, parentId: String?): Result<Collection> = resultOf {
        val dto = httpClient.post(COLLECTIONS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(NewCollectionDto(title = title, parent = parentId?.let { "/$COLLECTIONS_PATH/$it" }))
        }.body<CollectionDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun renameCollection(id: String, title: String): Result<Collection> =
        patch(id, CollectionPatchDto(title = title))

    /**
     * `POST` of one `multipart/form-data` part named `file`, the entity's file property
     * (`Collection::$file`), as `uploadItemImage` in `feature:items:data` sends it. The part
     * reads [bytes] again on each send, so the request is complete when `bearerAuth` repeats it
     * after a 401.
     */
    override suspend fun uploadCollectionImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Collection> = resultOf {
        val dto = httpClient.submitFormWithBinaryData(
            url = "$COLLECTIONS_PATH/$id/image",
            formData = formData {
                append(
                    key = "file",
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=${fileName.escapeIfNeeded()}")
                    }
                )
            }
        ).body<CollectionDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun deleteCollectionImage(id: String): Result<Collection> =
        patch(id, CollectionPatchDto(deleteImage = true))

    override suspend fun deleteCollection(id: String): Result<Unit> = resultOf {
        httpClient.delete("$COLLECTIONS_PATH/$id")
        Unit
    }

    private suspend fun patch(id: String, body: CollectionPatchDto): Result<Collection> = resultOf {
        val dto = httpClient.patch("$COLLECTIONS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(body)
        }.body<CollectionDto>()
        mapper.toDomain(dto, serverUrl())
    }

    private fun serverUrl(): String = normalizeBaseUrl(baseUrlProvider.getBaseUrl())
}

private const val COLLECTIONS_PATH = "api/collections"
