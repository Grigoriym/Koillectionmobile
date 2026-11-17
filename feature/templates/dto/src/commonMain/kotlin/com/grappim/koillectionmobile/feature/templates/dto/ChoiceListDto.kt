package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * A ChoiceList as `GET /api/choice_lists` sends it (`choiceList:read` group, Koillection 1.8.5).
 * Only the fields the app reads; the client's `Json` ignores the rest.
 */
@Serializable
data class ChoiceListDto(val id: String, val name: String, val choices: List<String>)
