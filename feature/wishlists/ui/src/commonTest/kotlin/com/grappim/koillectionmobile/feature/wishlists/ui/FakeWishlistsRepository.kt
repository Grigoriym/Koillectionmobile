package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Each list read takes the first queued response of its kind. Queue one before the call. A wishes
 * read for an id in [wishesOf] answers from there instead, for the parallel reads of a delete
 * count. Each wishlist write is logged in [writes] (`"create:<name>:<parent>"`,
 * `"rename:<id>:<name>"`, `"upload:<id>"`, `"deleteImage:<id>"`, `"delete:<id>"`) and answers with
 * its `...Answer`.
 */
class FakeWishlistsRepository : WishlistsRepository {
    val wishlists = ArrayDeque<CompletableDeferred<Result<List<Wishlist>>>>()
    val wishlist = ArrayDeque<CompletableDeferred<Result<Wishlist>>>()
    val children = ArrayDeque<CompletableDeferred<Result<List<Wishlist>>>>()
    val wishes = ArrayDeque<CompletableDeferred<Result<List<Wish>>>>()
    val wishesOf = mutableMapOf<String, Result<List<Wish>>>()

    var wishlistsCalls = 0
    val wishlistCalls = mutableListOf<String>()
    val childrenCalls = mutableListOf<String>()
    val wishesCalls = mutableListOf<String>()

    override suspend fun getWishlists(): Result<List<Wishlist>> {
        wishlistsCalls++
        return wishlists.first().await()
    }

    override suspend fun getWishlist(id: String): Result<Wishlist> {
        wishlistCalls += id
        return wishlist.first().await()
    }

    override suspend fun getChildren(id: String): Result<List<Wishlist>> {
        childrenCalls += id
        return children.first().await()
    }

    override suspend fun getWishes(wishlistId: String): Result<List<Wish>> {
        wishesCalls += wishlistId
        wishesOf[wishlistId]?.let { return it }
        return wishes.first().await()
    }

    val writes = mutableListOf<String>()
    val uploads = mutableListOf<Upload>()
    var createAnswer: suspend (name: String, parentId: String?) -> Result<Wishlist> =
        { name, parentId -> Result.success(wishlist(id = CREATED_ID, name = name, parentId = parentId)) }
    var renameAnswer: suspend () -> Result<Wishlist> = { Result.success(wishlist(id = "any", name = "any")) }
    var uploadAnswer: suspend () -> Result<Wishlist> = { Result.success(wishlist(id = "any", name = "any")) }
    var deleteImageAnswer: suspend () -> Result<Wishlist> = { Result.success(wishlist(id = "any", name = "any")) }
    var deleteAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }

    override suspend fun createWishlist(name: String, parentId: String?): Result<Wishlist> {
        writes += "create:$name:$parentId"
        return createAnswer(name, parentId)
    }

    override suspend fun renameWishlist(id: String, name: String): Result<Wishlist> {
        writes += "rename:$id:$name"
        return renameAnswer()
    }

    override suspend fun uploadWishlistImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Wishlist> {
        writes += "upload:$id"
        uploads += Upload(bytes, mimeType)
        return uploadAnswer()
    }

    override suspend fun deleteWishlistImage(id: String): Result<Wishlist> {
        writes += "deleteImage:$id"
        return deleteImageAnswer()
    }

    override suspend fun deleteWishlist(id: String): Result<Unit> {
        writes += "delete:$id"
        return deleteAnswer()
    }

    override suspend fun getWish(id: String): Result<Wish> = error("not used")

    override suspend fun createWish(
        wishlistId: String,
        name: String,
        url: String?,
        price: String?,
        currency: String?,
        comment: String?
    ): Result<Wish> = error("not used")

    override suspend fun editWish(id: String, changes: Map<WishField, String?>): Result<Wish> = error("not used")

    override suspend fun uploadWishImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Wish> = error("not used")

    override suspend fun deleteWish(id: String): Result<Unit> = error("not used")
}

class Upload(val bytes: ByteArray, val mimeType: String)

/** The id of a wishlist the fake creates. */
internal const val CREATED_ID = "created"

internal fun wishlist(id: String, name: String, parentId: String? = null) =
    Wishlist(id = id, name = name, parentId = parentId, color = null, imageUrl = null)

internal fun wish(id: String, name: String, wishlistId: String = "travel") = Wish(
    id = id,
    name = name,
    wishlistId = wishlistId,
    url = null,
    price = "12.50",
    currency = "EUR",
    comment = null,
    imageUrl = null,
    thumbnailUrl = null
)
