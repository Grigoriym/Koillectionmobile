package com.grappim.koillectionmobile.core.api

import com.grappim.kit.customheaders.CustomHeaders
import com.grappim.kit.customheaders.CustomHeadersStorage
import io.ktor.client.HttpClientConfig

/**
 * The configuration of the client that Coil loads images with (M14.13). Images are public files
 * under `uploads/` and need no token. A header-checking proxy in front of the server still rejects
 * a request without the user's custom headers, so the `CustomHeaders` plugin adds them, for the
 * server origin only.
 */
fun HttpClientConfig<*>.configureImageClient(
    baseUrlProvider: BaseUrlProvider,
    customHeadersStorage: CustomHeadersStorage
) {
    install(CustomHeaders) {
        storage = customHeadersStorage
        serverUrl = { normalizeBaseUrl(baseUrlProvider.getBaseUrl()) }
    }
}
