package com.grappim.koillectionmobile.core.image

/** The long side of an uploaded image, as in `add_book.py`. */
const val MAX_LONG_SIDE_PX = 2400

/** JPEG quality of an uploaded image, as in `add_book.py`. */
const val JPEG_QUALITY = 85

/** An extra clockwise rotation that the user picks, applied after the EXIF orientation. */
enum class Rotation(val degrees: Int) {
    NONE(0),
    CW_90(90),
    CW_180(180),
    CW_270(270)
}

data class Size(val width: Int, val height: Int)

/**
 * How to make the stored pixels upright: mirror horizontally (if [mirror]), then rotate
 * clockwise by [degrees].
 */
data class Orientation(val degrees: Int, val mirror: Boolean)

/** The EXIF `Orientation` tag (1..8) as an [Orientation]. An unknown value counts as 1. */
fun exifOrientation(tag: Int): Orientation = when (tag) {
    2 -> Orientation(degrees = 0, mirror = true)
    3 -> Orientation(degrees = 180, mirror = false)
    4 -> Orientation(degrees = 180, mirror = true)
    5 -> Orientation(degrees = 270, mirror = true)
    6 -> Orientation(degrees = 90, mirror = false)
    7 -> Orientation(degrees = 90, mirror = true)
    8 -> Orientation(degrees = 270, mirror = false)
    else -> Orientation(degrees = 0, mirror = false)
}

/** The EXIF orientation followed by the user's [extra] rotation. */
fun Orientation.then(extra: Rotation): Orientation = copy(degrees = (degrees + extra.degrees) % FULL_TURN)

/**
 * The size with the long side at most [maxLongSide], aspect ratio kept. A smaller image keeps
 * its size: no upscale, as ImageMagick's `-resize 2400x2400>`.
 */
fun Size.fitInside(maxLongSide: Int = MAX_LONG_SIDE_PX): Size {
    val longSide = maxOf(width, height)
    if (longSide <= maxLongSide) return this
    val scale = maxLongSide.toDouble() / longSide
    return Size(
        width = (width * scale).roundToPositiveInt(),
        height = (height * scale).roundToPositiveInt()
    )
}

/**
 * The largest power of 2 that keeps the decoded long side at least [maxLongSide], for
 * `BitmapFactory.Options.inSampleSize`. The exact downscale to [fitInside] comes after the decode.
 */
fun Size.sampleSize(maxLongSide: Int = MAX_LONG_SIDE_PX): Int {
    val longSide = maxOf(width, height)
    var sample = 1
    while (longSide / (sample * 2) >= maxLongSide) {
        sample *= 2
    }
    return sample
}

private fun Double.roundToPositiveInt(): Int = maxOf(1, kotlin.math.round(this).toInt())

private const val FULL_TURN = 360
