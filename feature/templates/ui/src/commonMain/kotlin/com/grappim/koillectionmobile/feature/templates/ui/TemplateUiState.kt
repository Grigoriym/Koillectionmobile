package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.uikit.NativeText

/** The fields of one template, in position order. A failed load or retry clears [fields] and sets [error]. */
data class TemplateUiState(
    val fields: List<TemplateFieldRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {}
)

/**
 * One field row. [type] is the server's `DatumTypeEnum` value. [choiceListName] is the name of
 * the field's choice list for a `choice-list` field, and null for every other type. It is also null
 * when the server has no choice list with the field's id.
 */
data class TemplateFieldRow(val id: String, val name: String, val type: String, val choiceListName: String?)
