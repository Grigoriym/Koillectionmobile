package com.grappim.koillectionmobile.feature.albums.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.BaseUrlProvider
import com.grappim.koillectionmobile.core.api.ContentTypeLdJson
import com.grappim.koillectionmobile.core.api.ContentTypeMergePatchJson
import com.grappim.koillectionmobile.core.api.getAllHydraPages
import com.grappim.koillectionmobile.core.api.normalizeBaseUrl
import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import com.grappim.koillectionmobile.feature.albums.dto.AlbumDto
import com.grappim.koillectionmobile.feature.albums.dto.AlbumPatchDto
import com.grappim.koillectionmobile.feature.albums.dto.NewAlbumDto
import com.grappim.koillectionmobile.feature.albums.dto.NewPhotoDto
import com.grappim.koillectionmobile.feature.albums.dto.PhotoDto
import com.grappim.koillectionmobile.feature.albums.dto.PhotoPatchKeys
import com.grappim.koillectionmobile.feature.albums.mapper.AlbumMapper
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
 * paths resolve against the same URL. A request body names an album by IRI (`/api/albums/<id>`).
 */
@Single(binds = [AlbumsRepository::class])
internal class AlbumsRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrlProvider: BaseUrlProvider,
    private val mapper: AlbumMapper
) : AlbumsRepository {

    override suspend fun getAlbums(): Result<List<Album>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<AlbumDto>(ALBUMS_PATH).map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getAlbum(id: String): Result<Album> = resultOf {
        mapper.toDomain(httpClient.get("$ALBUMS_PATH/$id").body<AlbumDto>(), serverUrl())
    }

    override suspend fun getChildren(id: String): Result<List<Album>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<AlbumDto>("$ALBUMS_PATH/$id/children").map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun createAlbum(title: String, parentId: String?): Result<Album> = resultOf {
        val dto = httpClient.post(ALBUMS_PATH) {
            contentType(ContentTypeLdJson)
            setBody(NewAlbumDto(title = title, parent = parentId?.let(::albumIri)))
        }.body<AlbumDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun renameAlbum(id: String, title: String): Result<Album> =
        patchAlbum(id, AlbumPatchDto(title = title))

    override suspend fun uploadAlbumImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Album> = resultOf {
        val dto = httpClient.uploadImage("$ALBUMS_PATH/$id/image", bytes, fileName, mimeType).body<AlbumDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun deleteAlbumImage(id: String): Result<Album> = patchAlbum(id, AlbumPatchDto(deleteImage = true))

    override suspend fun deleteAlbum(id: String): Result<Unit> = resultOf {
        httpClient.delete("$ALBUMS_PATH/$id")
        Unit
    }

    override suspend fun getPhotos(albumId: String): Result<List<Photo>> = resultOf {
        val serverUrl = serverUrl()
        httpClient.getAllHydraPages<PhotoDto>("$ALBUMS_PATH/$albumId/photos").map { mapper.toDomain(it, serverUrl) }
    }

    override suspend fun getPhoto(id: String): Result<Photo> = resultOf {
        mapper.toDomain(httpClient.get("$PHOTOS_PATH/$id").body<PhotoDto>(), serverUrl())
    }

    override suspend fun createPhoto(albumId: String, title: String, comment: String?, place: String?): Result<Photo> =
        resultOf {
            val dto = httpClient.post(PHOTOS_PATH) {
                contentType(ContentTypeLdJson)
                setBody(NewPhotoDto(title = title, album = albumIri(albumId), comment = comment, place = place))
            }.body<PhotoDto>()
            mapper.toDomain(dto, serverUrl())
        }

    /** A [JsonObject] body, so that a null value is written as `null` and an absent key is left out. */
    override suspend fun editPhoto(id: String, changes: Map<PhotoField, String?>): Result<Photo> = resultOf {
        val body = JsonObject(changes.entries.associate { (field, value) -> field.key() to JsonPrimitive(value) })
        val dto = httpClient.patch("$PHOTOS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(body)
        }.body<PhotoDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun uploadPhotoImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Photo> = resultOf {
        val dto = httpClient.uploadImage("$PHOTOS_PATH/$id/image", bytes, fileName, mimeType).body<PhotoDto>()
        mapper.toDomain(dto, serverUrl())
    }

    override suspend fun deletePhoto(id: String): Result<Unit> = resultOf {
        httpClient.delete("$PHOTOS_PATH/$id")
        Unit
    }

    /** Reified: `setBody` picks the serializer from the static type of [body]. */
    private suspend inline fun <reified T : Any> patchAlbum(id: String, body: T): Result<Album> = resultOf {
        val dto = httpClient.patch("$ALBUMS_PATH/$id") {
            contentType(ContentTypeMergePatchJson)
            setBody(body)
        }.body<AlbumDto>()
        mapper.toDomain(dto, serverUrl())
    }

    private fun serverUrl(): String = normalizeBaseUrl(baseUrlProvider.getBaseUrl())
}

/**
 * `POST` of one `multipart/form-data` part named `file`, the entity's file property
 * (`Album::$file`, `Photo::$file`), as `feature:wishlists:data` sends it. The part reads [bytes]
 * again on each send, so the request is complete when `bearerAuth` repeats it after a 401.
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

private fun PhotoField.key(): String = when (this) {
    PhotoField.TITLE -> PhotoPatchKeys.TITLE
    PhotoField.COMMENT -> PhotoPatchKeys.COMMENT
    PhotoField.PLACE -> PhotoPatchKeys.PLACE
}

private fun albumIri(id: String): String = "/$ALBUMS_PATH/$id"

private const val ALBUMS_PATH = "api/albums"
private const val PHOTOS_PATH = "api/photos"
