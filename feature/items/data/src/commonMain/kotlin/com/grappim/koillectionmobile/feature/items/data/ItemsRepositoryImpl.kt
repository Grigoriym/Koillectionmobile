package com.grappim.koillectionmobile.feature.items.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.core.api.normalizeBaseUrl
import com.grappim.koillectionmobile.feature.items.domain.Datum
import com.grappim.koillectionmobile.feature.items.domain.Item
import com.grappim.koillectionmobile.feature.items.domain.ItemsRepository
import com.grappim.koillectionmobile.feature.items.dto.DatumDto
import com.grappim.koillectionmobile.feature.items.dto.DatumPatchDto
import com.grappim.koillectionmobile.feature.items.dto.ItemDto
import com.grappim.koillectionmobile.feature.items.dto.ItemNamePatchDto
import com.grappim.koillectionmobile.feature.items.dto.NewDatumDto
import com.grappim.koillectionmobile.feature.items.dto.NewItemDto
import com.grappim.koillectionmobile.feature.items.mapper.ItemMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.escapeIfNeeded
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. Paths are relative to the stored server URL, and image
 * paths resolve against the same URL. A request body names other resources by IRI
 * (`/api/collections/<id>`), as `add_book.py` does.
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

    override suspend fun getAllItems(): Result<List<Item>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<ItemDto>(ITEMS_PATH).map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getTagItems(tagId: String): Result<List<Item>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<ItemDto>("api/tags/$tagId/items").map { mapper.toDomain(it, serverUrl) }
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

    override suspend fun createItem(collectionId: String, name: String): Result<Item> = resultOf {
        val dto = httpClient.post(ITEMS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(NewItemDto(name = name, collection = "/api/collections/$collectionId"))
        }.body<ItemDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun createTextDatum(itemId: String, label: String, value: String, position: Int): Result<Datum> =
        createDatum(
            NewDatumDto(item = itemIri(itemId), type = "text", label = label, position = position, value = value)
        )

    override suspend fun createImageDatum(itemId: String, label: String, position: Int): Result<Datum> =
        createDatum(NewDatumDto(item = itemIri(itemId), type = "image", label = label, position = position))

    private suspend fun createDatum(body: NewDatumDto): Result<Datum> = resultOf {
        val dto = httpClient.post(DATA_PATH) {
            contentType(ContentTypeLdJson)
            setBody(body)
        }.body<DatumDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun uploadItemImage(
        itemId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Item> = resultOf {
        val dto = uploadImage("$ITEMS_PATH/$itemId/image", "file", bytes, fileName, mimeType).body<ItemDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun uploadDatumImage(
        datumId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Datum> = resultOf {
        val dto = uploadImage("$DATA_PATH/$datumId/image", "fileImage", bytes, fileName, mimeType).body<DatumDto>()
        mapper.toDomain(dto, serverUrl())
    }

    /**
     * `POST` of one `multipart/form-data` part named [field]. The field name is the entity's file
     * property (`Item::$file`, `Datum::$fileImage`). The part reads [bytes] again on each send,
     * so the request is complete when `bearerAuth` repeats it after a 401.
     */
    private suspend fun uploadImage(
        path: String,
        field: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): HttpResponse = httpClient.submitFormWithBinaryData(
        url = path,
        formData = formData {
            append(
                key = field,
                value = bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=${fileName.escapeIfNeeded()}")
                }
            )
        }
    )

    override suspend fun renameItem(id: String, name: String): Result<Item> = resultOf {
        val dto = httpClient.patch("$ITEMS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(ItemNamePatchDto(name = name))
        }.body<ItemDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun updateDatum(id: String, label: String?, value: String?): Result<Datum> = resultOf {
        val dto = httpClient.patch("$DATA_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(DatumPatchDto(label = label, value = value))
        }.body<DatumDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun deleteDatum(id: String): Result<Unit> = resultOf {
        httpClient.delete("$DATA_PATH/$id")
        Unit
    }

    override suspend fun deleteItem(id: String): Result<Unit> = resultOf {
        httpClient.delete("$ITEMS_PATH/$id")
        Unit
    }

    private fun itemIri(itemId: String): String = "/$ITEMS_PATH/$itemId"

    private fun serverUrl(): String = normalizeBaseUrl(baseUrlProvider.getBaseUrl())
}

private const val ITEMS_PATH = "api/items"
private const val DATA_PATH = "api/data"
