package com.grappim.koillectionmobile.feature.tags.dto

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/tags` (`tag:write` group, Koillection 1.8.5). `label` is the only field
 * the server requires (`#[Assert\NotBlank]`). The app sends no category, and the server keeps its
 * default visibility.
 */
@Serializable
data class NewTagDto(val label: String)
