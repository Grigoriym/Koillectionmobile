package com.grappim.koillectionmobile.core.ui

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/**
 * The strings of a nested delete. [text] takes the container's name, then the [children] count
 * and the [contents] count, each already formatted.
 */
data class DeleteTexts(
    val title: StringResource,
    val text: StringResource,
    val children: PluralStringResource,
    val contents: PluralStringResource,
    val failedTitle: StringResource
)
