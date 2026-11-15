package com.grappim.koillectionmobile.feature.items.domain

/**
 * One field of an item. [type] is the server's type name (`text`, `textarea`, `image`, ...).
 * [value] is null for the file types. [image] is set only for a field that holds an uploaded
 * image (type `image` or `sign`).
 */
data class Datum(
    val id: String,
    val type: String,
    val label: String?,
    val value: String?,
    val position: Int?,
    val image: Image?
)
