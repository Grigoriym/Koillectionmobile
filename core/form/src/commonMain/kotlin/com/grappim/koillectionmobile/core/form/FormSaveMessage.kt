package com.grappim.koillectionmobile.core.form

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_validation
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected

/** The message of a failed form save. An upload 500 usually means a full upload quota (plan §2), so that one is named. */
fun Throwable.toSaveMessage(uploadFailed: Boolean): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.collection_form_save_error_network)

    is KoillectionError.Server -> if (uploadFailed && status == HTTP_INTERNAL_ERROR) {
        NativeText.Resource(RString.collection_form_save_error_quota)
    } else {
        NativeText.Arguments(RString.collection_form_save_error_http, listOf(status))
    }

    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))

    is KoillectionError.Http -> NativeText.Arguments(RString.collection_form_save_error_http, listOf(status))

    is KoillectionError.Unauthorized ->
        NativeText.Arguments(RString.collection_form_save_error_http, listOf(HTTP_UNAUTHORIZED))

    is KoillectionError.NotFound -> NativeText.Arguments(
        RString.collection_form_save_error_http,
        listOf(HTTP_NOT_FOUND)
    )

    is KoillectionError.Validation -> NativeText.Resource(RString.collection_form_save_error_validation)

    else -> NativeText.Resource(RString.collection_form_save_error_unexpected)
}

private const val HTTP_INTERNAL_ERROR = 500
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_NOT_FOUND = 404
