package com.grappim.koillectionmobile.feature.tags.domain

/** A group of tags. [color] is the server's hex value without `#` (`F3E5F5`). */
data class TagCategory(val id: String, val label: String?, val description: String?, val color: String?)
