package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.core.image.Rotation

/**
 * The add-item photos as files on the device (`PhotoRow.uri`). The platform part, bound by
 * [PlatformPhotoModule].
 */
interface PhotoFiles {

    /**
     * The upload bytes of the photo at [uri]: EXIF orientation, then [rotation], downscaled, JPEG
     * ([JPEG_MIME_TYPE]). Throws when the photo cannot be read.
     */
    suspend fun prepare(uri: String, rotation: Rotation): ByteArray

    /** Deletes the photo files. A file that is already gone is not an error. Blocking, but cheap. */
    fun delete(uris: List<String>)
}

internal const val JPEG_MIME_TYPE = "image/jpeg"

/** Binds [PhotoFiles] for the platform. The app's `AppModule` includes it. */
expect class PlatformPhotoModule
