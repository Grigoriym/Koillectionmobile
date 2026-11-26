package com.grappim.koillectionmobile.feature.albums.ui

/**
 * Sorts by the title that [title] picks, ignoring case. The web UI sorts albums by title
 * (`AlbumRepository`); it lists photos in database order, which the API does not show, so the app
 * sorts photos by title too.
 */
internal fun <T> byTitle(title: (T) -> String): Comparator<T> = compareBy(String.CASE_INSENSITIVE_ORDER, title)
