package com.grappim.koillectionmobile.feature.wishlists.domain

/**
 * Access to the user's wishlists and wishes. A failure is a `KoillectionError` (`core:domain`)
 * inside the [Result].
 */
interface WishlistsRepository {

    /**
     * Every wishlist of the user, from all pages. Plan §2 records whether the server also lists
     * the child wishlists (M9.2). A top-level wishlist has no `parentId`.
     */
    suspend fun getWishlists(): Result<List<Wishlist>>

    suspend fun getWishlist(id: String): Result<Wishlist>

    /** The direct children of wishlist [id], from all pages. */
    suspend fun getChildren(id: String): Result<List<Wishlist>>

    /**
     * Creates a wishlist named [name] and returns it as the server sent it. A null [parentId]
     * makes a top-level wishlist; otherwise the new one is a child of [parentId].
     */
    suspend fun createWishlist(name: String, parentId: String? = null): Result<Wishlist>

    /** Renames wishlist [id] and returns the wishlist as the server sent it. */
    suspend fun renameWishlist(id: String, name: String): Result<Wishlist>

    /**
     * Uploads [bytes] as the image of wishlist [id] and returns the wishlist with its new image.
     * An existing image is replaced. [mimeType] is one the server takes: `image/png`,
     * `image/jpeg`, `image/webp` or `image/avif`. [fileName] only fills the multipart part.
     */
    suspend fun uploadWishlistImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<Wishlist>

    /** Removes the image of wishlist [id] and returns the wishlist as the server sent it. */
    suspend fun deleteWishlistImage(id: String): Result<Wishlist>

    /** Deletes wishlist [id]. The server deletes its child wishlists and their wishes with it. */
    suspend fun deleteWishlist(id: String): Result<Unit>

    /** The wishes directly in wishlist [wishlistId], not in its children, from all pages. */
    suspend fun getWishes(wishlistId: String): Result<List<Wish>>

    suspend fun getWish(id: String): Result<Wish>

    /**
     * Creates a wish named [name] in wishlist [wishlistId] and returns it as the server sent it. A
     * null field is left out of the body.
     */
    suspend fun createWish(
        wishlistId: String,
        name: String,
        url: String? = null,
        price: String? = null,
        currency: String? = null,
        comment: String? = null
    ): Result<Wish>

    /**
     * Changes the fields of wish [id] that [changes] names, and only those. A null value is sent
     * as an explicit `null`. Returns the wish as the server sent it.
     */
    suspend fun editWish(id: String, changes: Map<WishField, String?>): Result<Wish>

    /**
     * Uploads [bytes] as the image of wish [id] and returns the wish with its new image. An
     * existing image is replaced. [mimeType] and [fileName] as in [uploadWishlistImage].
     */
    suspend fun uploadWishImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<Wish>

    suspend fun deleteWish(id: String): Result<Unit>
}
