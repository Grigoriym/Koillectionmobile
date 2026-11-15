package com.grappim.koillectionmobile.feature.collections.domain

/**
 * A Koillection collection. Collections nest: a top-level one has no [parentId].
 *
 * [imageUrl] is a full URL. The server sends a path relative to its root, and the mapper resolves
 * it against the stored server URL.
 */
data class Collection(
    val id: String,
    val title: String,
    val parentId: String?,
    val color: String?,
    val imageUrl: String?
)
