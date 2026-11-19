package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.feature.items.domain.TEXT_TYPES
import com.grappim.koillectionmobile.feature.items.domain.TYPE_IMAGE
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField

/**
 * What a template adds to the add form: the empty typed [rows], the empty image [slots], and the
 * [skipped] fields of a type the form cannot fill (`sign`, `file`, `video`, `section`,
 * `blank-line`; user, 2026-11-19).
 */
internal class AppliedTemplate(val rows: List<FieldRow>, val slots: List<ImageSlot>, val skipped: List<TemplateField>)

/**
 * The template [fields] (in position order) as form rows. A field whose name is in [usedLabels],
 * or is the name of an earlier field of the template, is not added: the server takes each label
 * once per item. The names are compared exactly, as the server does (`===`).
 */
internal fun applyTemplate(
    fields: List<TemplateField>,
    usedLabels: Set<String>,
    nextRowId: () -> Int,
    nextSlotId: () -> Int
): AppliedTemplate {
    val labels = usedLabels.toMutableSet()
    val rows = mutableListOf<FieldRow>()
    val slots = mutableListOf<ImageSlot>()
    val skipped = mutableListOf<TemplateField>()
    for (field in fields) {
        when {
            field.type != TYPE_IMAGE && field.type !in TEXT_TYPES -> skipped += field

            !labels.add(field.name) -> Unit

            field.type == TYPE_IMAGE -> slots += ImageSlot(id = nextSlotId(), label = field.name)

            else -> rows += FieldRow(
                id = nextRowId(),
                label = field.name,
                value = emptyValue(field.type),
                type = field.type,
                choiceListId = field.choiceListId
            )
        }
    }
    return AppliedTemplate(rows, slots, skipped)
}
