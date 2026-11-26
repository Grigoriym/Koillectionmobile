package com.grappim.koillectionmobile.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteSummaryTest {

    @Test
    fun `descendantIds finds every level under the root and nothing else`() {
        val all = listOf(
            "root" to null,
            "a" to "root",
            "b" to "root",
            "a1" to "a",
            "a1x" to "a1",
            "other" to null,
            "other1" to "other"
        )

        assertEquals(setOf("a", "b", "a1", "a1x"), descendantIds("root", all).toSet())
    }

    @Test
    fun `descendantIds of a leaf is empty`() {
        assertEquals(emptyList(), descendantIds("a", listOf("root" to null, "a" to "root")))
    }
}
