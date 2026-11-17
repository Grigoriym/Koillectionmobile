package com.grappim.koillectionmobile.feature.tags.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * The tags of one category, sorted by label. [label] and [color] are the category's; both are null
 * for the tags without a category. [color] is the server's hex value without `#`, as in
 * [TagCategory].
 */
data class TagGroup(val label: String?, val color: String?, val tags: List<Tag>)

/** The server's tags and categories, read in parallel and grouped by [groupTags]. */
suspend fun TagsRepository.loadTagGroups(): Result<List<TagGroup>> = coroutineScope {
    val tags = async { getTags() }
    val categories = async { getTagCategories() }
    val tagsResult = tags.await()
    val categoriesResult = categories.await()
    val failure = tagsResult.exceptionOrNull() ?: categoriesResult.exceptionOrNull()
    if (failure != null) {
        Result.failure(failure)
    } else {
        Result.success(groupTags(tagsResult.getOrThrow(), categoriesResult.getOrThrow()))
    }
}

/**
 * One group per category that has tags, sorted by category label; tags without a category (or
 * with a category the list does not hold) go last. Tags in a group are sorted by label. Both sorts
 * ignore case.
 */
fun groupTags(tags: List<Tag>, categories: List<TagCategory>): List<TagGroup> {
    val categoryById = categories.associateBy { it.id }
    val byCategory = tags.groupBy { tag -> tag.categoryId?.let { categoryById[it] } }
    val categorized = byCategory.mapNotNull { (category, list) ->
        category?.let { TagGroup(label = it.label.orEmpty(), color = it.color, tags = list.sortedByLabel()) }
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label.orEmpty() })
    val uncategorized = byCategory[null]?.let { TagGroup(label = null, color = null, tags = it.sortedByLabel()) }
    return categorized + listOfNotNull(uncategorized)
}

private fun List<Tag>.sortedByLabel(): List<Tag> = sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
