package com.grappim.koillectionmobile.feature.collections.domain

/**
 * Read access to the user's collections. A failure is a `KoillectionError` (`core:domain`) inside
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
}
