package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.core.photos.PhotoFiles

/** A photo's bytes are `"<uri>@<rotation>"`, so a test can see which photo went where. */
class FakePhotoFiles : PhotoFiles {
    val prepared = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    val unreadable = mutableSetOf<String>()

    override suspend fun prepare(uri: String, rotation: Rotation): ByteArray {
        if (uri in unreadable) error("Cannot read $uri")
        prepared += uri
        return "$uri@$rotation".encodeToByteArray()
    }

    override fun delete(uris: List<String>) {
        deleted += uris
    }
}
