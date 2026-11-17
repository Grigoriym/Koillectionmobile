package com.grappim.koillectionmobile.feature.templates.domain

/**
 * A field of a template.
 *
 * - [type] is a `DatumTypeEnum::TYPES` value as the server sends it (`text`, `number`,
 *   `checkbox`, `choice-list`, ...).
 * - [choiceListId] is set for a `choice-list` field, and null otherwise.
 */
data class TemplateField(
    val id: String,
    val name: String,
    val position: Int,
    val type: String,
    val choiceListId: String?
)
