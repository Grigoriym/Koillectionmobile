package com.grappim.koillectionmobile.feature.wishlists.ui

/** Sorts by the name that [name] picks, ignoring case, as the web UI lists wishes. */
internal fun <T> byName(name: (T) -> String): Comparator<T> = compareBy(String.CASE_INSENSITIVE_ORDER, name)
