package com.grappim.koillectionmobile.feature.templates.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/choice_lists` and of `PATCH /api/choice_lists/{id}` (`choiceList:write`
 * group, Koillection 1.8.5). Both properties are always sent. In a merge patch an array replaces
 * the stored one, so [choices] is the full list in its order. The server rejects a duplicate
 * choice (`#[Assert\Unique]`).
 */
@Serializable
data class ChoiceListWriteDto(val name: String, val choices: List<String>)
