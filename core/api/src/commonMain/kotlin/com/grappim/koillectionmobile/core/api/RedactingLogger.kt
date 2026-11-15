package com.grappim.koillectionmobile.core.api

import com.grappim.kit.logger.logcat
import io.ktor.client.plugins.logging.Logger

/**
 * At `LogLevel.ALL` Ktor writes bodies verbatim. The login request body holds the password and
 * the login response body holds the JWT, and `sanitizeHeader` reaches neither of them.
 */
internal class RedactingLogger(private val tag: String) : Logger {
    override fun log(message: String) {
        logcat(tag = tag) { redactSecrets(message) }
    }
}

/** A JSON `"password": "..."` member. The value may hold escaped quotes. */
private val PASSWORD_MEMBER = Regex(""""password"\s*:\s*"(?:[^"\\]|\\.)*"""")

/**
 * Any JWT, wherever it appears: the `token` member of the login response, or a bearer header
 * that a sanitizer missed. A JWT header is base64url JSON, so it always starts with `eyJ`.
 */
private val JWT = Regex("""eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*""")

internal fun redactSecrets(message: String): String = message
    .replace(PASSWORD_MEMBER, "\"password\":\"REDACTED\"")
    .replace(JWT, "REDACTED")
