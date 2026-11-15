package com.grappim.koillectionmobile.feature.tags.domain

/** A Koillection tag. [categoryId] is null when the tag has no category. */
data class Tag(val id: String, val label: String, val description: String?, val categoryId: String?)
