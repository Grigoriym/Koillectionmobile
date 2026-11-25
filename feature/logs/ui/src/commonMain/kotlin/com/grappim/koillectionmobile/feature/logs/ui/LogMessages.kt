package com.grappim.koillectionmobile.feature.logs.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.logs_error_http
import com.grappim.koillectionmobile.strings.generated.resources.logs_error_network
import com.grappim.koillectionmobile.strings.generated.resources.logs_error_unexpected

/** The message of a failed page read. */
internal fun Throwable.toMessage(): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.logs_error_network)
    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
    is KoillectionError.Server -> NativeText.Arguments(RString.logs_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.logs_error_http, listOf(status))
    else -> NativeText.Resource(RString.logs_error_unexpected)
}
