package com.grappim.koillectionmobile.core.api

import com.grappim.kit.customheaders.CustomHeader
import com.grappim.kit.testing.FakeCustomHeadersStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageHttpClientTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(baseUrl: String): HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            respondOk()
        }
    ) {
        configureImageClient(
            baseUrlProvider = object : BaseUrlProvider {
                override fun getBaseUrl(): String = baseUrl
            },
            customHeadersStorage = FakeCustomHeadersStorage(listOf(CustomHeader("X-Proxy-Token", "secret")))
        )
    }

    @Test
    fun `an image on the server gets the custom header`() = runTest {
        client("https://koi.example/sub").get("https://koi.example/sub/uploads/a.jpg")

        assertEquals("secret", requests.single().headers["X-Proxy-Token"])
    }

    @Test
    fun `an image on a root-path server gets the custom header`() = runTest {
        client("https://koi.example").get("https://koi.example/uploads/a.jpg")

        assertEquals("secret", requests.single().headers["X-Proxy-Token"])
    }

    @Test
    fun `an image on another host does not get the custom header`() = runTest {
        client("https://koi.example/sub").get("https://other.example/uploads/a.jpg")

        assertNull(requests.single().headers["X-Proxy-Token"])
    }
}
