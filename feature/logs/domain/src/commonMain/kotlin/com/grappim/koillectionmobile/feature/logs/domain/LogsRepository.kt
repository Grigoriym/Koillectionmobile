package com.grappim.koillectionmobile.feature.logs.domain

/**
 * Read access to the user's logs, newest first. The API sends them oldest first (plan §5
 * decision 16), so the first call reads the last page and each later call reads the page before.
 * A failure is a `KoillectionError` (`core:domain`) inside the [Result].
 */
interface LogsRepository {

    /**
     * Reads one page. [path] is null for the first call; after that it is the `nextPath` of
     * the page before.
     */
    suspend fun getLogs(path: String?): Result<LogPage>
}
