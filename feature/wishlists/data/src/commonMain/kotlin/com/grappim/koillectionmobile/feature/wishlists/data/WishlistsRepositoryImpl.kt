package com.grappim.koillectionmobile.feature.wishlists.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.core.api.normalizeBaseUrl
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import com.grappim.koillectionmobile.feature.wishlists.dto.NewWishDto
import com.grappim.koillectionmobile.feature.wishlists.dto.NewWishlistDto
import com.grappim.koillectionmobile.feature.wishlists.dto.WishDto
import com.grappim.koillectionmobile.feature.wishlists.dto.WishPatchKeys
import com.grappim.koillectionmobile.feature.wishlists.dto.WishlistDto
import com.grappim.koillectionmobile.feature.wishlists.dto.WishlistPatchDto
import com.grappim.koillectionmobile.feature.wishlists.mapper.WishlistMapper
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. Paths are relative to the stored server URL, and image
 * paths resolve against the same URL. A request body names a wishlist by IRI
 * (`/api/wishlists/<id>`).
 */
@Single(binds = [WishlistsRepository::class])
internal class WishlistsRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrlProvider: BaseUrlProvider,
    private val mapper: WishlistMapper
) : WishlistsRepository {

    override suspend fun getWishlists(): Result<List<Wishlist>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<WishlistDto>(WISHLISTS_PATH).map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getWishlist(id: String): Result<Wishlist> = resultOf {
        mapper.toDomain(httpClient.get("$WISHLISTS_PATH/$id").body<WishlistDto>(), serverUrl())
    }

    override suspend fun getChildren(id: String): Result<List<Wishlist>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<WishlistDto>("$WISHLISTS_PATH/$id/children")
            .map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun createWishlist(name: String, parentId: String?): Result<Wishlist> = resultOf {
        val dto = httpClient.post(WISHLISTS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(NewWishlistDto(name = name, parent = parentId?.let(::wishlistIri)))
        }.body<WishlistDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun renameWishlist(id: String, name: String): Result<Wishlist> =
        patchWishlist(id, WishlistPatchDto(name = name))

    override suspend fun uploadWishlistImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Wishlist> = resultOf {
        val dto = httpClient.uploadImage("$WISHLISTS_PATH/$id/image", bytes, fileName, mimeType).body<WishlistDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun deleteWishlistImage(id: String): Result<Wishlist> =
        patchWishlist(id, WishlistPatchDto(deleteImage = true))

    override suspend fun deleteWishlist(id: String): Result<Unit> = resultOf {
        httpClient.delete("$WISHLISTS_PATH/$id")
        Unit
    }

    override suspend fun getWishes(wishlistId: String): Result<List<Wish>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<WishDto>("$WISHLISTS_PATH/$wishlistId/wishes")
            .map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getWish(id: String): Result<Wish> = resultOf {
        mapper.toDomain(httpClient.get("$WISHES_PATH/$id").body<WishDto>(), serverUrl())
    }

    override suspend fun createWish(
        wishlistId: String,
        name: String,
        url: String?,
        price: String?,
        currency: String?,
        comment: String?
    ): Result<Wish> = resultOf {
        val dto = httpClient.post(WISHES_PATH) {
            contentType(ContentTypeLdJson)
            setBody(
                NewWishDto(
                    name = name,
                    wishlist = wishlistIri(wishlistId),
                    url = url,
                    price = price,
                    currency = currency,
                    comment = comment
                )
            )
        }.body<WishDto>()
        mapper.toDomain(dto, serverUrl())
    }

    /** A [JsonObject] body, so that a null value is written as `null` and an absent key is left out. */
    override suspend fun editWish(id: String, changes: Map<WishField, String?>): Result<Wish> = resultOf {
        val body = JsonObject(changes.entries.associate { (field, value) -> field.key() to JsonPrimitive(value) })
        val dto = httpClient.patch("$WISHES_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(body)
        }.body<WishDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun uploadWishImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Wish> = resultOf {
        val dto = httpClient.uploadImage("$WISHES_PATH/$id/image", bytes, fileName, mimeType).body<WishDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun deleteWish(id: String): Result<Unit> = resultOf {
        httpClient.delete("$WISHES_PATH/$id")
        Unit
    }

    /** Reified: `setBody` picks the serializer from the static type of [body]. */
    private suspend inline fun <reified T : Any> patchWishlist(id: String, body: T): Result<Wishlist> = resultOf {
        val dto = httpClient.patch("$WISHLISTS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(body)
        }.body<WishlistDto>()
        mapper.toDomain(dto, serverUrl())
    }

    private fun serverUrl(): String = normalizeBaseUrl(baseUrlProvider.getBaseUrl())
}

/**
 * `POST` of one `multipart/form-data` part named `file`, the entity's file property
 * (`Wishlist::$file`, `Wish::$file`), as `uploadCollectionImage` in `feature:collections:data`
 * sends it. The part reads [bytes] again on each send, so the request is complete when
 * `bearerAuth` repeats it after a 401.
 */
private suspend fun HttpClient.uploadImage(path: String, bytes: ByteArray, fileName: String, mimeType: String) =
    submitFormWithBinaryData(
        url = path,
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
    )

private fun WishField.key(): String = when (this) {
    WishField.NAME -> WishPatchKeys.NAME
    WishField.URL -> WishPatchKeys.URL
    WishField.PRICE -> WishPatchKeys.PRICE
    WishField.CURRENCY -> WishPatchKeys.CURRENCY
    WishField.COMMENT -> WishPatchKeys.COMMENT
}

private fun wishlistIri(id: String): String = "/$WISHLISTS_PATH/$id"

private const val WISHLISTS_PATH = "api/wishlists"
private const val WISHES_PATH = "api/wishes"
