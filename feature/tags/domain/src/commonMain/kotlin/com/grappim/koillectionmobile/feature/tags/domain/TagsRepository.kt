package com.grappim.koillectionmobile.feature.tags.domain

/**
 * Read access to tags and tag categories. A failure is a `KoillectionError` (`core:domain`)
 * inside the [Result].
 */
interface TagsRepository {

    /** All tags of the user, from all pages. */
    suspend fun getTags(): Result<List<Tag>>

    /** All tag categories of the user, from all pages. */
    suspend fun getTagCategories(): Result<List<TagCategory>>

    /** The tags of item [itemId], from all pages. */
    suspend fun getItemTags(itemId: String): Result<List<Tag>>
}
