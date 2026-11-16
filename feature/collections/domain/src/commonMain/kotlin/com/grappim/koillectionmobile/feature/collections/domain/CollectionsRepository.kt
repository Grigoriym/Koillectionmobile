package com.grappim.koillectionmobile.feature.collections.domain

/**
 * Access to the user's collections. A failure is a `KoillectionError` (`core:domain`) inside
 * the [Result].
 */
interface CollectionsRepository {

    /**
     * Every collection of the user, at every level, from all pages. The server has no filter for
     * the top level: keep the ones without a `parentId`.
     */
    suspend fun getCollections(): Result<List<Collection>>

    suspend fun getCollection(id: String): Result<Collection>

    /** The direct children of collection [id], from all pages. */
    suspend fun getChildren(id: String): Result<List<Collection>>

    /**
     * Creates a collection titled [title] and returns it as the server sent it. A null [parentId]
     * makes a top-level collection; otherwise the new one is a child of [parentId].
     */
    suspend fun createCollection(title: String, parentId: String? = null): Result<Collection>

    /** Renames collection [id] and returns the collection as the server sent it. */
    suspend fun renameCollection(id: String, title: String): Result<Collection>

    /**
     * Uploads [bytes] as the image of collection [id] and returns the collection with its new
     * image. An existing image is replaced. [mimeType] is one the server takes: `image/png`,
     * `image/jpeg`, `image/webp` or `image/avif`. The server stores the file under its own name;
     * [fileName] only fills the multipart part.
     */
    suspend fun uploadCollectionImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Collection>

    /** Removes the image of collection [id] and returns the collection as the server sent it. */
    suspend fun deleteCollectionImage(id: String): Result<Collection>

    /**
     * Deletes collection [id]. The server deletes its child collections (every level) and their
     * items with it.
     */
    suspend fun deleteCollection(id: String): Result<Unit>
}
