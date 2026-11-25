package com.grappim.koillectionmobile.feature.logs.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Log as `GET /api/logs` sends it (`log:read` group, Koillection 1.8.5). Only the fields the
 * app reads; the client's `Json` ignores the rest.
 *
 * - [logType] is the JSON key `type`: `create` or `delete` (`LogTypeEnum`). The column is
 *   nullable. It is not named `type`, which `CLAUDE.md` keeps off `@Serializable` classes.
 * - [objectClass] is the full PHP class name, for example `App\Entity\Item`.
 * - [loggedAt] is a date-time with the server's offset (`2026-10-21T11:18:43+02:00`).
 */
@Serializable
data class LogDto(
    val id: String,
    @SerialName("type")
    val logType: String? = null,
    val loggedAt: String,
    val objectLabel: String,
    val objectClass: String
)
