package com.grappim.koillectionmobile.feature.collections.domain

/**
 * A Koillection collection. Collections nest: a top-level one has no [parentId].
 *
 * [imageUrl] is a full URL. The server sends a path relative to its root, and the mapper resolves
 * it against the stored server URL. [defaultTemplateId] is the template the add form in this collection
 * applies when it opens, or null.
 */
data class Collection(
    val id: String,
    val title: String,
    val parentId: String?,
    val color: String?,
    val imageUrl: String?,
    val defaultTemplateId: String? = null
)
