package com.grappim.koillectionmobile.feature.items.dto

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/data/{id}` (`datum:write` group, Koillection 1.8.5). A merge patch: a
 * null property is left out of the body, so the server keeps that value. A new [label] must not
 * be the label of another field of the same item (`DatumLabelNotExistsInParent`).
 */
@Serializable
data class DatumPatchDto(val label: String? = null, val value: String? = null)
