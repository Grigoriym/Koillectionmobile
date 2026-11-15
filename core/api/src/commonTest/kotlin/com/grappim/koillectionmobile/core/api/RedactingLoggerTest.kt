package com.grappim.koillectionmobile.core.api

import kotlin.test.Test
import kotlin.test.assertEquals

class RedactingLoggerTest {

    @Test
    fun `a password member is redacted including escaped quotes`() {
        val message = """BODY {"username":"tester","password" : "a\"b c"}"""

        assertEquals("""BODY {"username":"tester","password":"REDACTED"}""", redactSecrets(message))
    }

    @Test
    fun `a JWT is redacted wherever it appears`() {
        val jwt = "eyJ0eXAiOiJKV1QifQ.eyJ1c2VybmFtZSI6InRlc3RlciJ9.sig-_part"

        assertEquals("""{"token":"REDACTED"}""", redactSecrets("""{"token":"$jwt"}"""))
        assertEquals("-> Authorization: Bearer REDACTED", redactSecrets("-> Authorization: Bearer $jwt"))
    }

    @Test
    fun `other text is left alone`() {
        val message = """{"title":"Books","id":"01a1de6f-bfad-78b6-a916-7266d9e3dfff"}"""

        assertEquals(message, redactSecrets(message))
    }
}
