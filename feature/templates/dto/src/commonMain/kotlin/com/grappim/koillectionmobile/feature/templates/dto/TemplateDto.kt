package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * A Template as `GET /api/templates` sends it (`template:read` group, Koillection 1.8.5). Only the
 * fields the app reads; the client's `Json` ignores the rest. The fields are not in the group:
 * they come from `GET /api/templates/{id}/fields`.
 */
@Serializable
data class TemplateDto(val id: String, val name: String)
