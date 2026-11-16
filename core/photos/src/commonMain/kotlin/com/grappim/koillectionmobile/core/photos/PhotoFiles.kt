package com.grappim.koillectionmobile.core.photos

import com.grappim.koillectionmobile.core.image.Rotation

/**
 * The form photos as files on the device (a content URI each). The platform part, bound by
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

const val JPEG_MIME_TYPE = "image/jpeg"

/** Binds [PhotoFiles] for the platform. The app's `AppModule` includes it. */
expect class PlatformPhotoModule

/** The next clockwise quarter turn: 0, 90, 180, 270, then 0 again. */
fun Rotation.next(): Rotation = Rotation.entries[(ordinal + 1) % Rotation.entries.size]
