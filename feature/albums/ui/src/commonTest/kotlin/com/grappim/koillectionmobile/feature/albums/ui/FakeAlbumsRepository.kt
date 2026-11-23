package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.koillectionmobile.feature.albums.domain.Album
import com.grappim.koillectionmobile.feature.albums.domain.AlbumsRepository
import com.grappim.koillectionmobile.feature.albums.domain.Photo
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import kotlinx.coroutines.CompletableDeferred

/**
 * Each read takes the first queued response of its kind. Queue one before the call. The browse
 * screens send no write, so each write fails the test.
 */
class FakeAlbumsRepository : AlbumsRepository {
    val albums = ArrayDeque<CompletableDeferred<Result<List<Album>>>>()
    val album = ArrayDeque<CompletableDeferred<Result<Album>>>()
    val children = ArrayDeque<CompletableDeferred<Result<List<Album>>>>()
    val photos = ArrayDeque<CompletableDeferred<Result<List<Photo>>>>()

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
        return photos.first().await()
    }

    override suspend fun createAlbum(title: String, parentId: String?): Result<Album> = unexpected()

    override suspend fun renameAlbum(id: String, title: String): Result<Album> = unexpected()

    override suspend fun uploadAlbumImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Album> = unexpected()

    override suspend fun deleteAlbumImage(id: String): Result<Album> = unexpected()

    override suspend fun deleteAlbum(id: String): Result<Unit> = unexpected()

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

    private fun unexpected(): Nothing = error("The browse screens send no write")
}

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
