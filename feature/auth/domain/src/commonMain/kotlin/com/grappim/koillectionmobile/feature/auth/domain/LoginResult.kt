package com.grappim.koillectionmobile.feature.auth.domain

import com.grappim.koillectionmobile.core.domain.KoillectionError

/**
 * How a login attempt ended. Each value except [Failed] needs its own message in the UI, which is
 * the only reason they are separate.
 */
sealed interface LoginResult {

    /** The server URL, the credentials and the JWT are stored. */
    data object LoggedIn : LoginResult

    /** The URL is not `http://host...` or `https://host...`. No request was sent. */
    data object InvalidServerUrl : LoginResult

    /** No response: the host does not resolve, refuses the connection, or times out. */
    data object Unreachable : LoginResult

    /**
     * Something answered, but not Koillection's login endpoint: a 404 or 405, or a success body
     * without a `token`. Usually a wrong path in the URL (a missing or extra subpath).
     */
    data object NotKoillection : LoginResult

    /** 401. Koillection gives the same answer for a wrong password and an unknown user. */
    data object InvalidCredentials : LoginResult

    /**
     * 500 at login. Usually the server's JWT keys are broken, for example after a container restart
     * made a new `JWT_PASSPHRASE` (plan §2 trap). The website still works in that state, so the UI
     * must say so rather than show a generic error.
     */
    data object ServerJwtFailure : LoginResult

    /** Any other server error, for example a 502 from a reverse proxy. */
    data class Failed(val error: KoillectionError) : LoginResult
}
