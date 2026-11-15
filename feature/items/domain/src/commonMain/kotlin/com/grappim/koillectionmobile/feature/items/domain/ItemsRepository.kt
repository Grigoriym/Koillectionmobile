package com.grappim.koillectionmobile.feature.items.domain

/**
 * Access to items and their fields. A failure is a `KoillectionError` (`core:domain`) inside
 * the [Result].
 */
interface ItemsRepository {

    /** The items of collection [collectionId], from all pages. */
    suspend fun getItems(collectionId: String): Result<List<Item>>

    suspend fun getItem(id: String): Result<Item>

    /** The fields of item [itemId], sorted by `position`. A field without a position comes last. */
    suspend fun getData(itemId: String): Result<List<Datum>>

    /** Creates an item named [name] in collection [collectionId] and returns it as the server sent it. */
    suspend fun createItem(collectionId: String, name: String): Result<Item>

    /** Adds a `text` field to item [itemId]. [label] must not be in use on that item yet. */
    suspend fun createTextDatum(itemId: String, label: String, value: String, position: Int): Result<Datum>

    /**
     * Adds an `image` field to item [itemId], without the image. The image goes up in a second
     * call, to the returned field's id. [label] must not be in use on that item yet.
     */
    suspend fun createImageDatum(itemId: String, label: String, position: Int): Result<Datum>
}
