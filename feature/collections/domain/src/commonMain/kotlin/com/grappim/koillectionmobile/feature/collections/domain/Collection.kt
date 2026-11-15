package com.grappim.koillectionmobile.feature.collections.domain

/**
 * A Koillection collection. Collections nest: a top-level one has no [parentId].
 *
 * [imagePath] is relative to the server root (`uploads/...`). It is not resolved to a full URL
 * here.
 */
data class Collection(
    val id: String,
    val title: String,
    val parentId: String?,
    val color: String?,
    val imagePath: String?
)
