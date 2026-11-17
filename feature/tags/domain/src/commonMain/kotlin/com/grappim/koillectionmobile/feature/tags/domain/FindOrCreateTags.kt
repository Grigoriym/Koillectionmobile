package com.grappim.koillectionmobile.feature.tags.domain

/**
 * The ids of the tags with [labels], in order. A tag that exists with the label (ignoring case)
 * is used; a missing one is created. The server's tags are read first, so a retry after a create
 * that went through does not create the tag a second time.
 */
suspend fun TagsRepository.findOrCreateTags(labels: List<String>): Result<List<String>> {
    val known = getTags().getOrElse { return Result.failure(it) }.toMutableList()
    val ids = labels.map { label ->
        val tag = known.findByLabel(label) ?: createTag(label).getOrElse { return Result.failure(it) }
        known += tag
        tag.id
    }
    return Result.success(ids)
}

/** The first tag with [label], ignoring case. */
fun List<Tag>.findByLabel(label: String): Tag? = firstOrNull { it.label.equals(label, ignoreCase = true) }
