package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * A template Field as `GET /api/templates/{id}/fields` sends it (`field:read` group, Koillection
 * 1.8.5). Only the fields the app reads; the client's `Json` ignores the rest.
 *
 * - [type] is one of `DatumTypeEnum::TYPES` (`text`, `number`, `checkbox`, `choice-list`, ...).
 * - [choiceList] is an IRI (`/api/choice_lists/<id>`), or null.
 */
@Serializable
data class FieldDto(
    val id: String,
    val name: String,
    val position: Int,
    val type: String,
    val choiceList: String? = null
)
