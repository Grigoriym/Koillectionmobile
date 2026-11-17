package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/templates` and of `PATCH /api/templates/{id}` (`template:write` group,
 * Koillection 1.8.5). `name` is the only property in the group (`#[Assert\NotBlank]`).
 */
@Serializable
data class TemplateNameDto(val name: String)
