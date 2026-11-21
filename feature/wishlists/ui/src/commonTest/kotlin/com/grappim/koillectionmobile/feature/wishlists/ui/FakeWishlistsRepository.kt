package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.feature.wishlists.domain.Wishlist
import com.grappim.koillectionmobile.feature.wishlists.domain.WishlistsRepository
import kotlinx.coroutines.CompletableDeferred

/** Each read takes the first queued response of its kind. Queue one before the call. */
class FakeWishlistsRepository : WishlistsRepository {
    val wishlists = ArrayDeque<CompletableDeferred<Result<List<Wishlist>>>>()
    val wishlist = ArrayDeque<CompletableDeferred<Result<Wishlist>>>()
    val children = ArrayDeque<CompletableDeferred<Result<List<Wishlist>>>>()
    val wishes = ArrayDeque<CompletableDeferred<Result<List<Wish>>>>()

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
        return wishes.first().await()
    }

    override suspend fun createWishlist(name: String, parentId: String?): Result<Wishlist> = error("not used")

    override suspend fun renameWishlist(id: String, name: String): Result<Wishlist> = error("not used")

    override suspend fun uploadWishlistImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Wishlist> = error("not used")

    override suspend fun deleteWishlistImage(id: String): Result<Wishlist> = error("not used")

    override suspend fun deleteWishlist(id: String): Result<Unit> = error("not used")

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
