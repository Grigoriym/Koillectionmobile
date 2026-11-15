package com.grappim.koillectionmobile.feature.auth.dto

import kotlinx.serialization.Serializable

/** The body of `POST /api/authentication_token` (plan §2). */
@Serializable
data class LoginRequestDto(val username: String, val password: String) {
    /** The password stays out of logs and crash reports. */
    override fun toString(): String = "LoginRequestDto(username=$username, password=REDACTED)"
}

/** The success body of `POST /api/authentication_token`: `{"token": "<JWT>"}`. */
@Serializable
data class LoginResponseDto(val token: String) {
    override fun toString(): String = "LoginResponseDto(token=REDACTED)"
}
