package com.grappim.koillectionmobile.feature.items.ui

/** What a [FieldRowEditor] reports. The screen binds each one to its row. */
internal class FieldRowActions(
    val onLabelChange: (String) -> Unit,
    val onValueChange: (String) -> Unit,
    val onTypeChange: (String) -> Unit,
    val onChoiceListChange: (String) -> Unit,
    val onCheckedChange: (Boolean) -> Unit,
    val onChoiceToggle: (String) -> Unit,
    val onRetryChoiceLists: () -> Unit,
    val onRemove: () -> Unit
)
