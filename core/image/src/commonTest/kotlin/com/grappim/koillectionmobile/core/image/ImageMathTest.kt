package com.grappim.koillectionmobile.core.image

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageMathTest {
    @Test
    fun `exif orientation maps each tag to a rotation and a mirror`() {
        val expected =
            mapOf(
                1 to Orientation(0, false),
                2 to Orientation(0, true),
                3 to Orientation(180, false),
                4 to Orientation(180, true),
                5 to Orientation(270, true),
                6 to Orientation(90, false),
                7 to Orientation(90, true),
                8 to Orientation(270, false)
            )
        expected.forEach { (tag, orientation) -> assertEquals(orientation, exifOrientation(tag), "tag $tag") }
    }

    @Test
    fun `unknown exif tag counts as normal`() {
        assertEquals(Orientation(0, false), exifOrientation(0))
        assertEquals(Orientation(0, false), exifOrientation(9))
    }

    @Test
    fun `extra rotation adds clockwise and wraps at 360`() {
        assertEquals(Orientation(180, false), Orientation(90, false).then(Rotation.CW_90))
        assertEquals(Orientation(0, true), Orientation(270, true).then(Rotation.CW_90))
        assertEquals(Orientation(180, false), Orientation(270, false).then(Rotation.CW_270))
        assertEquals(Orientation(90, false), Orientation(90, false).then(Rotation.NONE))
    }

    @Test
    fun `fitInside scales the long side down to 2400`() {
        assertEquals(Size(2400, 1800), Size(4000, 3000).fitInside())
        assertEquals(Size(1800, 2400), Size(3000, 4000).fitInside())
    }

    @Test
    fun `fitInside rounds the short side`() {
        // 4032 x 3024 (a 12 MP phone photo): 3024 * 2400 / 4032 = 1800 exactly.
        assertEquals(Size(2400, 1800), Size(4032, 3024).fitInside())
        // 1000 * 2400 / 3001 = 799.73
        assertEquals(Size(2400, 800), Size(3001, 1000).fitInside())
    }

    @Test
    fun `fitInside keeps a short side of at least 1 px`() {
        assertEquals(Size(2400, 1), Size(10000, 1).fitInside())
    }

    @Test
    fun `fitInside does not upscale`() {
        assertEquals(Size(2400, 1000), Size(2400, 1000).fitInside())
        assertEquals(Size(800, 600), Size(800, 600).fitInside())
    }

    @Test
    fun `sampleSize keeps the decoded long side at least 2400`() {
        assertEquals(1, Size(2400, 1800).sampleSize())
        assertEquals(1, Size(4799, 3000).sampleSize())
        assertEquals(2, Size(4800, 3000).sampleSize())
        assertEquals(2, Size(3000, 9599).sampleSize())
        assertEquals(4, Size(3000, 9600).sampleSize())
    }

    @Test
    fun `sampleSize of a small image is 1`() {
        assertEquals(1, Size(800, 600).sampleSize())
    }
}
