package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/fields` (`field:write` group, Koillection 1.8.5). `name`, `position`,
 * `type` and `template` are `#[Assert\NotBlank]`. The app sends no `visibility`: the server keeps
 * `public`.
 *
 * - [template] is an IRI (`/api/templates/<id>`).
 * - [choiceList] is an IRI (`/api/choice_lists/<id>`). The server requires it when [type] is
 *   `choice-list` (`Assert\When`). A null is left out of the body.
 */
@Serializable
data class NewFieldDto(
    val name: String,
    val position: Int,
    val type: String,
    val template: String,
    val choiceList: String? = null
)
