package com.grappim.koillectionmobile.feature.albums.domain

/**
 * Access to the user's albums and photos. A failure is a `KoillectionError` (`core:domain`) inside
 * the [Result].
 */
interface AlbumsRepository {

    /** Every album of the user from all pages. A top-level album has no `parentId`. */
    suspend fun getAlbums(): Result<List<Album>>

    suspend fun getAlbum(id: String): Result<Album>

    /** The direct children of album [id], from all pages. */
    suspend fun getChildren(id: String): Result<List<Album>>

    /**
     * Creates an album titled [title] and returns it as the server sent it. A null [parentId] makes
     * a top-level album; otherwise the new one is a child of [parentId].
     */
    suspend fun createAlbum(title: String, parentId: String? = null): Result<Album>

    /** Renames album [id] and returns the album as the server sent it. */
    suspend fun renameAlbum(id: String, title: String): Result<Album>

    /**
     * Uploads [bytes] as the image of album [id] and returns the album with its new image. An
     * existing image is replaced. [mimeType] is one the server takes: `image/png`, `image/jpeg`,
     * `image/webp` or `image/avif`. [fileName] only fills the multipart part.
     */
    suspend fun uploadAlbumImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<Album>

    /** Removes the image of album [id] and returns the album as the server sent it. */
    suspend fun deleteAlbumImage(id: String): Result<Album>

    /** Deletes album [id]. The server deletes its child albums and their photos with it. */
    suspend fun deleteAlbum(id: String): Result<Unit>

    /** The photos directly in album [albumId], not in its children, from all pages. */
    suspend fun getPhotos(albumId: String): Result<List<Photo>>

    suspend fun getPhoto(id: String): Result<Photo>

    /**
     * Creates a photo titled [title] in album [albumId] and returns it as the server sent it. A
     * null field is left out of the body.
     */
    suspend fun createPhoto(
        albumId: String,
        title: String,
        comment: String? = null,
        place: String? = null
    ): Result<Photo>

    /**
     * Changes the fields of photo [id] that [changes] names, and only those. A null value is sent
     * as an explicit `null`. Returns the photo as the server sent it.
     */
    suspend fun editPhoto(id: String, changes: Map<PhotoField, String?>): Result<Photo>

    /**
     * Uploads [bytes] as the image of photo [id] and returns the photo with its new image. An
     * existing image is replaced. [mimeType] and [fileName] as in [uploadAlbumImage].
     */
    suspend fun uploadPhotoImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<Photo>

    suspend fun deletePhoto(id: String): Result<Unit>
}
