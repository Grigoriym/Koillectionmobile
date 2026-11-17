package com.grappim.koillectionmobile.feature.items.domain

/**
 * Access to items and their fields. A failure is a `KoillectionError` (`core:domain`) inside
 * the [Result].
 */
interface ItemsRepository {

    /** The items of collection [collectionId], from all pages. */
    suspend fun getItems(collectionId: String): Result<List<Item>>

    /**
     * Every item of the user, in every collection, from all pages. The server has no name filter:
     * search in the app.
     */
    suspend fun getAllItems(): Result<List<Item>>

    /** The items that carry tag [tagId], in every collection, from all pages. */
    suspend fun getTagItems(tagId: String): Result<List<Item>>

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

    /**
     * Uploads [bytes] as the main image of item [itemId] and returns the item with its new image.
     * [mimeType] is one the server takes: `image/png`, `image/jpeg`, `image/webp` or `image/avif`.
     * The server stores the file under its own name; [fileName] only fills the multipart part.
     */
    suspend fun uploadItemImage(itemId: String, bytes: ByteArray, fileName: String, mimeType: String): Result<Item>

    /**
     * Uploads [bytes] as the image of `image` field [datumId] (from [createImageDatum]) and returns
     * the field with its new image. [mimeType] and [fileName] as in [uploadItemImage].
     */
    suspend fun uploadDatumImage(datumId: String, bytes: ByteArray, fileName: String, mimeType: String): Result<Datum>

    /** Renames item [id] and returns the item as the server sent it. */
    suspend fun renameItem(id: String, name: String): Result<Item>

    /**
     * Changes the [label] and/or the [value] of field [id] and returns the field as the server sent
     * it. A null argument is not sent, so the server keeps that value. [label] must not be in use
     * by another field of the same item.
     */
    suspend fun updateDatum(id: String, label: String? = null, value: String? = null): Result<Datum>

    /** Deletes field [id]. The server deletes its file with it. */
    suspend fun deleteDatum(id: String): Result<Unit>

    /** Deletes item [id]. The server deletes its fields and files with it. */
    suspend fun deleteItem(id: String): Result<Unit>
}
