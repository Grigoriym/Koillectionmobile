package com.grappim.koillectionmobile.core.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_http
import com.grappim.koillectionmobile.strings.generated.resources.error_network
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.logs_error_unexpected
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {

    private val unexpected = RString.logs_error_unexpected

    @Test
    fun `a network error names the network`() {
        val message = KoillectionError.Network(IllegalStateException("down")).toMessage(unexpected)

        assertEquals(NativeText.Resource(RString.error_network), message)
    }

    @Test
    fun `a server error and another HTTP error show the status`() {
        assertEquals(
            NativeText.Arguments(RString.error_http, listOf(503)),
            KoillectionError.Server(503, null).toMessage(unexpected)
        )
        assertEquals(
            NativeText.Arguments(RString.error_http, listOf(403)),
            KoillectionError.Http(403, null).toMessage(unexpected)
        )
    }

    @Test
    fun `a proxy rejection names the proxy`() {
        assertEquals(
            NativeText.Arguments(RString.error_proxy_rejected, listOf(407)),
            KoillectionError.ProxyRejection(407).toMessage(unexpected)
        )
    }

    @Test
    fun `any other error shows the text of the caller`() {
        assertEquals(NativeText.Resource(unexpected), KoillectionError.NotFound(null).toMessage(unexpected))
        assertEquals(NativeText.Resource(unexpected), IllegalStateException().toMessage(unexpected))
    }
}
