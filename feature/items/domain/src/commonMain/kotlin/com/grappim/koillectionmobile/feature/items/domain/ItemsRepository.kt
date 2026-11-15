package com.grappim.koillectionmobile.feature.items.domain

/**
 * Read access to items and their fields. A failure is a `KoillectionError` (`core:domain`) inside
 * the [Result].
 */
interface ItemsRepository {

    /** The items of collection [collectionId], from all pages. */
    suspend fun getItems(collectionId: String): Result<List<Item>>

    suspend fun getItem(id: String): Result<Item>

    /** The fields of item [itemId], sorted by `position`. A field without a position comes last. */
    suspend fun getData(itemId: String): Result<List<Datum>>
}
