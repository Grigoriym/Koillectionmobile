package com.grappim.koillectionmobile.feature.items.domain

/** A Koillection item. [image] is the main image, or null when the item has none. */
data class Item(val id: String, val name: String, val quantity: Int, val collectionId: String?, val image: Image?)
