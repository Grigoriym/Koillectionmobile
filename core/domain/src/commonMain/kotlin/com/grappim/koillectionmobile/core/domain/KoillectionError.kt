package com.grappim.koillectionmobile.core.domain

/**
 * Every failure of a call to the Koillection server. `core:api` throws it for a non-2xx status
 * and for a network failure, so callers catch one type.
 *
 * [message] is the server's own text where the body has one (`message` from the JWT layer,
 * `detail` from API Platform, plan §2). It is English and meant for logs, not for the UI.
 */
sealed class KoillectionError(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** 401: no token, a bad token, or wrong credentials at login. */
    class Unauthorized(message: String?) : KoillectionError(message)

    /** 404: the resource does not exist, or the user does not own it. */
    class NotFound(message: String?) : KoillectionError(message)

    /** 400 or 422: the server rejected the request body. */
    class Validation(message: String?) : KoillectionError(message)

    /**
     * 5xx. At login a 500 usually means the server's JWT keys are broken (plan §2 trap), so
     * the status is kept.
     */
    class Server(val status: Int, message: String?) : KoillectionError(message)

    /**
     * A proxy in front of the server rejected the request, so it did not reach Koillection: a 401
     * that is not the JWT layer's, a 407, or a redirect to another host. Usually the proxy wants a
     * custom header that the app does not send.
     */
    class ProxyRejection(val status: Int) : KoillectionError("Rejected by a proxy (HTTP $status)")

    /** Any other non-2xx status, for example 403, 405 or 413. */
    class Http(val status: Int, message: String?) : KoillectionError(message)

    /** No response: the server is unreachable, the name does not resolve, or a timeout. */
    class Network(cause: Throwable) : KoillectionError(cause.message, cause)
}
