package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/fields/{id}` (`field:write` group, Koillection 1.8.5). A merge patch: a
 * null property is left out of the body, so the server keeps that value.
 *
 * - [choiceList] is an IRI (`/api/choice_lists/<id>`).
 */
@Serializable
data class FieldPatchDto(
    val name: String? = null,
    val position: Int? = null,
    val type: String? = null,
    val choiceList: String? = null
)
