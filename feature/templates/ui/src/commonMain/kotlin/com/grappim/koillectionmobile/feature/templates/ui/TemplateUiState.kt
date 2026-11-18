package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.uikit.NativeText

/**
 * The fields of one template, in position order. A failed load or retry clears [fields] and sets
 * [error]. [name] is the template's name as loaded; null until the first load.
 *
 * [isDeleting] is true while the delete runs. [isDeleted] is set once the server deleted the
 * template: the screen then leaves. [deleteError] is the message of a failed delete.
 */
data class TemplateUiState(
    val name: String? = null,
    val fields: List<TemplateFieldRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onDeleteErrorDismiss: () -> Unit = {}
) {
    /** The template is loaded and no delete runs or has run. */
    val canChange: Boolean get() = name != null && !isLoading && error.isEmpty() && !isDeleting && !isDeleted
}

/**
 * One field row. [type] is the server's `DatumTypeEnum` value. [choiceListName] is the name of
 * the field's choice list for a `choice-list` field, and null for every other type. It is also null
 * when the server has no choice list with the field's id.
 */
data class TemplateFieldRow(val id: String, val name: String, val type: String, val choiceListName: String?)
