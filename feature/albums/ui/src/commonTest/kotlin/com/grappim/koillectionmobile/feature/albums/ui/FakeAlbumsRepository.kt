package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import kotlinx.coroutines.CompletableDeferred

/**
 * Each read takes the first queued response of its kind. Queue one before the call. A [photosOf] entry
 * answers the photos of that album instead of the queue. Each album write is logged in [writes]
 * (`"create:<title>:<parent>"`, `"rename:<id>:<title>"`, `"upload:<id>"`, `"deleteImage:<id>"`,
 * `"delete:<id>"`) and answers with its `...Answer`. No screen sends the other writes yet, so each
 * of them fails the test.
 */
class FakeAlbumsRepository : AlbumsRepository {
    val albums = ArrayDeque<CompletableDeferred<Result<List<Album>>>>()
    val album = ArrayDeque<CompletableDeferred<Result<Album>>>()
    val children = ArrayDeque<CompletableDeferred<Result<List<Album>>>>()
    val photos = ArrayDeque<CompletableDeferred<Result<List<Photo>>>>()
    val photosOf = mutableMapOf<String, Result<List<Photo>>>()

    var albumsCalls = 0
    val albumCalls = mutableListOf<String>()
    val childrenCalls = mutableListOf<String>()
    val photosCalls = mutableListOf<String>()

    override suspend fun getAlbums(): Result<List<Album>> {
        albumsCalls++
        return albums.first().await()
    }

    override suspend fun getAlbum(id: String): Result<Album> {
        albumCalls += id
        return album.first().await()
    }

    override suspend fun getChildren(id: String): Result<List<Album>> {
        childrenCalls += id
        return children.first().await()
    }

    override suspend fun getPhotos(albumId: String): Result<List<Photo>> {
        photosCalls += albumId
        photosOf[albumId]?.let { return it }
        return photos.first().await()
    }

    val writes = mutableListOf<String>()
    val uploads = mutableListOf<Upload>()
    var createAnswer: suspend (title: String, parentId: String?) -> Result<Album> =
        { title, parentId -> Result.success(album(id = CREATED_ID, title = title, parentId = parentId)) }
    var renameAnswer: suspend () -> Result<Album> = { Result.success(album(id = "any", title = "any")) }
    var uploadAnswer: suspend () -> Result<Album> = { Result.success(album(id = "any", title = "any")) }
    var deleteImageAnswer: suspend () -> Result<Album> = { Result.success(album(id = "any", title = "any")) }
    var deleteAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }

    override suspend fun createAlbum(title: String, parentId: String?): Result<Album> {
        writes += "create:$title:$parentId"
        return createAnswer(title, parentId)
    }

    override suspend fun renameAlbum(id: String, title: String): Result<Album> {
        writes += "rename:$id:$title"
        return renameAnswer()
    }

    override suspend fun uploadAlbumImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Album> {
        writes += "upload:$id"
        uploads += Upload(bytes, mimeType)
        return uploadAnswer()
    }

    override suspend fun deleteAlbumImage(id: String): Result<Album> {
        writes += "deleteImage:$id"
        return deleteImageAnswer()
    }

    override suspend fun deleteAlbum(id: String): Result<Unit> {
        writes += "delete:$id"
        return deleteAnswer()
    }

    override suspend fun getPhoto(id: String): Result<Photo> = unexpected()

    override suspend fun createPhoto(albumId: String, title: String, comment: String?, place: String?): Result<Photo> =
        unexpected()

    override suspend fun editPhoto(id: String, changes: Map<PhotoField, String?>): Result<Photo> = unexpected()

    override suspend fun uploadPhotoImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Photo> = unexpected()

    override suspend fun deletePhoto(id: String): Result<Unit> = unexpected()

    private fun unexpected(): Nothing = error("No screen sends this write yet")
}

class Upload(val bytes: ByteArray, val mimeType: String)

internal const val CREATED_ID = "created"

internal fun album(id: String, title: String, parentId: String? = null) =
    Album(id = id, title = title, parentId = parentId, color = null, imageUrl = null)

internal fun photo(id: String, title: String, albumId: String = "trips") = Photo(
    id = id,
    title = title,
    albumId = albumId,
    comment = null,
    place = null,
    imageUrl = null,
    thumbnailUrl = null,
    takenAt = null
)
