package com.grappim.koillectionmobile.feature.tags.domain

/**
 * Access to tags and tag categories, and to an item's tags. A failure is a `KoillectionError` (`core:domain`)
 * inside the [Result].
 */
interface TagsRepository {

    /** All tags of the user, from all pages. */
    suspend fun getTags(): Result<List<Tag>>

    /** All tag categories of the user, from all pages. */
    suspend fun getTagCategories(): Result<List<TagCategory>>

    /** The tags of item [itemId], from all pages. */
    suspend fun getItemTags(itemId: String): Result<List<Tag>>

    /**
     * Sets the tags of item [itemId] to exactly [tagIds]. The API takes only the full list, so a
     * tag left out is removed, and an empty list removes every tag.
     */
    suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit>
}
