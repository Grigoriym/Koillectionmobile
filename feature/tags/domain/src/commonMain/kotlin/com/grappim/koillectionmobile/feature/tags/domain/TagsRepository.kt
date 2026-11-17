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

    /**
     * Creates a tag with [label] only and returns it as the server sent it. The tag has no
     * category, and the server sets the visibility. A blank [label] fails with a validation error.
     */
    suspend fun createTag(label: String): Result<Tag>

    /** The tags of item [itemId], from all pages. */
    suspend fun getItemTags(itemId: String): Result<List<Tag>>

    /**
     * Sets the tags of item [itemId] to exactly [tagIds]. The API takes only the full list, so a
     * tag left out is removed, and an empty list removes every tag.
     */
    suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit>
}
