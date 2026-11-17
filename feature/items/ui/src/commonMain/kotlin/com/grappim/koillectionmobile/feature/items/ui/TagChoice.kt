package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.feature.tags.domain.TagGroup
import com.grappim.koillectionmobile.feature.tags.domain.findByLabel

/** The tags of a form: the ids of picked server tags, and the labels of tags the save creates. */
internal data class TagChoice(val selectedTagIds: Set<String>, val newTagLabels: List<String>)

/**
 * Adds [label] (trimmed) for a new tag. When a tag of [tagGroups] has the label (ignoring case),
 * that tag is picked instead, and nothing is created. A blank label, or one already pending
 * (ignoring case), changes nothing.
 */
internal fun TagChoice.withNewTagLabel(label: String, tagGroups: List<TagGroup>): TagChoice {
    val trimmed = label.trim()
    val existing = tagGroups.flatMap { it.tags }.findByLabel(trimmed)
    return when {
        trimmed.isEmpty() -> this
        existing != null -> copy(selectedTagIds = selectedTagIds + existing.id)
        newTagLabels.any { it.equals(trimmed, ignoreCase = true) } -> this
        else -> copy(newTagLabels = newTagLabels + trimmed)
    }
}
