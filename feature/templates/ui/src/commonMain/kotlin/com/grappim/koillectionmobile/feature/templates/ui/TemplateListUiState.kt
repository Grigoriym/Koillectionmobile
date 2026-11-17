package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.Template

/** The templates of the user, sorted by name ignoring case. A failed load or retry clears [templates] and sets [error]. */
data class TemplateListUiState(
    val templates: List<Template> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)
