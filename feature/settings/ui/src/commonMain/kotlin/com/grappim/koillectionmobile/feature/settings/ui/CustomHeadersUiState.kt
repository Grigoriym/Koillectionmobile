package com.grappim.koillectionmobile.feature.settings.ui

import com.grappim.kit.customheaders.CustomHeader
import com.grappim.kit.customheaders.CustomHeaderError
import com.grappim.kit.customheaders.isBlank

/**
 * [rows] is what the user edits; [saved] is what storage last emitted. Save is offered only while
 * the two differ, so a screen with nothing changed has nothing to press.
 */
data class CustomHeadersUiState(
    val rows: List<CustomHeaderRow> = emptyList(),
    val saved: List<CustomHeader> = emptyList(),
    val isSaving: Boolean = false,
    val onAdd: () -> Unit = {},
    val onRemove: (Int) -> Unit = {},
    val onNameChange: (Int, String) -> Unit = { _, _ -> },
    val onValueChange: (Int, String) -> Unit = { _, _ -> },
    val onSaveClick: () -> Unit = {}
) {
    /** The headers a save would store: trimmed, with unfilled rows dropped. */
    val headers: List<CustomHeader>
        get() = rows.map { CustomHeader(it.name.trim(), it.value.trim()) }.filterNot { it.isBlank() }

    val isSaveEnabled: Boolean
        get() = !isSaving && rows.none { it.error != null } && headers != saved
}

/** One editable header. [error] is the validator's verdict on the row as it is now. */
data class CustomHeaderRow(val name: String = "", val value: String = "", val error: CustomHeaderError? = null)
