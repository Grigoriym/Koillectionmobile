package com.grappim.koillectionmobile.core.api

import kotlin.test.Test
import kotlin.test.assertEquals

class BaseUrlProviderTest {

    @Test
    fun `the base URL gets exactly one trailing slash`() {
        assertEquals("https://koi.example/sub/", normalizeBaseUrl(" https://koi.example/sub// "))
        assertEquals("https://koi.example/", normalizeBaseUrl("https://koi.example"))
        assertEquals("", normalizeBaseUrl("  "))
    }
}
