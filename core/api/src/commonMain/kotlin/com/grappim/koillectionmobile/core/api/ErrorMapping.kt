package com.grappim.koillectionmobile.core.api

import com.grappim.koillectionmobile.core.domain.KoillectionError
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Maps a non-2xx response to a [KoillectionError]. [body] is the raw response text.
 */
internal fun mapHttpError(status: Int, body: String, json: Json): KoillectionError {
    val message = errorMessage(body, json)
    return when (status) {
        HttpStatusCode.Unauthorized.value -> KoillectionError.Unauthorized(message)

        HttpStatusCode.NotFound.value -> KoillectionError.NotFound(message)

        HttpStatusCode.BadRequest.value,
        HttpStatusCode.UnprocessableEntity.value -> KoillectionError.Validation(message)

        in SERVER_ERRORS -> KoillectionError.Server(status, message)

        else -> KoillectionError.Http(status, message)
    }
}

/**
 * The server sends three error shapes (checked against 1.8.5, 2026-11-15):
 * - the JWT layer: `{"code": 401, "message": "Invalid credentials."}`
 * - API Platform: `{"title": "An error occurred", "detail": "Not Found", "description": ...}`
 * - older API Platform (Hydra prefix): `{"hydra:description": ...}`
 *
 * A body that is not a JSON object (a proxy's HTML page, for example) gives no message.
 */
private fun errorMessage(body: String, json: Json): String? {
    val obj = try {
        json.parseToJsonElement(body).jsonObject
    } catch (_: IllegalArgumentException) {
        // `SerializationException` (bad JSON) and the non-object case both extend it.
        return null
    }
    return MESSAGE_KEYS.firstNotNullOfOrNull { obj.stringOrNull(it) }
}

private fun JsonObject.stringOrNull(key: String): String? =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

private val MESSAGE_KEYS = listOf("message", "detail", "description", "hydra:description")

private val SERVER_ERRORS = 500..599
