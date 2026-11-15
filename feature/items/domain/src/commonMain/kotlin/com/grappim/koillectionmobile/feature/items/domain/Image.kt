package com.grappim.koillectionmobile.feature.items.domain

/**
 * An uploaded image as full URLs on the user's server. The server serves `uploads/` as static
 * files, without the JWT. A thumbnail URL is null when the server sent no thumbnail.
 */
data class Image(val url: String, val smallThumbnailUrl: String?, val largeThumbnailUrl: String?)
