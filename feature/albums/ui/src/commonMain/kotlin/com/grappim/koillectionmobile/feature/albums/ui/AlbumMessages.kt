package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_http
import com.grappim.koillectionmobile.strings.generated.resources.albums_error_network
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import org.jetbrains.compose.resources.StringResource

/** The message of a failed load; [unexpected] is the text for an error the server did not send. */
internal fun Throwable.toMessage(unexpected: StringResource): NativeText = when (this) {
    is KoillectionError.Network -> NativeText.Resource(RString.albums_error_network)
    is KoillectionError.ProxyRejection -> NativeText.Arguments(RString.error_proxy_rejected, listOf(status))
    is KoillectionError.Server -> NativeText.Arguments(RString.albums_error_http, listOf(status))
    is KoillectionError.Http -> NativeText.Arguments(RString.albums_error_http, listOf(status))
    else -> NativeText.Resource(unexpected)
}

/**
 * Sorts by the title that [title] picks, ignoring case. The web UI sorts albums by title
 * (`AlbumRepository`); it lists photos in database order, which the API does not show, so the app
 * sorts photos by title too.
 */
internal fun <T> byTitle(title: (T) -> String): Comparator<T> = compareBy(String.CASE_INSENSITIVE_ORDER, title)
