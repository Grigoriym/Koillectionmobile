package com.grappim.koillectionmobile.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Turns a photo at a content URI into upload bytes: EXIF orientation, then [Rotation], long side
 * at most [MAX_LONG_SIDE_PX], JPEG at [JPEG_QUALITY]. Blocking: call it off the main thread.
 */
class ImagePreparer(private val contentResolver: ContentResolver) {
    fun prepare(uri: Uri, extra: Rotation): ByteArray {
        val upright = bitmap(uri, extra)
        return ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            upright.recycle()
            out.toByteArray()
        }
    }

    /**
     * The upright bitmap that [prepare] encodes, with the long side at most [maxLongSide]. The
     * add-item preview calls it with a small [maxLongSide], so it shows what the upload gets.
     */
    fun bitmap(uri: Uri, extra: Rotation, maxLongSide: Int = MAX_LONG_SIDE_PX): Bitmap {
        val exifTag =
            open(uri).use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        val orientation = exifOrientation(exifTag).then(extra)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val source = Size(bounds.outWidth, bounds.outHeight)
        if (source.width <= 0 || source.height <= 0) throw IOException("Not an image: $uri")

        val options = BitmapFactory.Options().apply { inSampleSize = source.sampleSize(maxLongSide) }
        val decoded =
            open(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IOException("Cannot decode $uri")

        val target = Size(decoded.width, decoded.height).fitInside(maxLongSide)
        val scaled = Bitmap.createScaledBitmap(decoded, target.width, target.height, true)
        if (scaled !== decoded) decoded.recycle()

        val upright = scaled.transformed(orientation)
        if (upright !== scaled) scaled.recycle()
        return upright
    }

    private fun open(uri: Uri): InputStream =
        contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")

    private fun Bitmap.transformed(orientation: Orientation): Bitmap {
        if (orientation.degrees == 0 && !orientation.mirror) return this
        val matrix =
            Matrix().apply {
                if (orientation.mirror) postScale(-1f, 1f)
                postRotate(orientation.degrees.toFloat())
            }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
