package com.grappim.koillectionmobile.core.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_http
import com.grappim.koillectionmobile.strings.generated.resources.error_network
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import org.jetbrains.compose.resources.StringResource

/** The message of a failed load; [unexpected] is the text for an error the server did not send. */
fun Throwable.toMessage(unexpected: StringResource): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.error_network)
    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
    is KoillectionError.Server -> NativeText.Arguments(RString.error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.error_http, listOf(status))
    else -> NativeText.Resource(unexpected)
}
