package com.grappim.koillectionmobile.feature.logs.ui

import com.grappim.koillectionmobile.feature.logs.domain.LogAction
import com.grappim.koillectionmobile.feature.logs.domain.LogEntry
import com.grappim.koillectionmobile.feature.logs.domain.LogObjectType
import com.grappim.koillectionmobile.feature.logs.domain.LogPage
import com.grappim.koillectionmobile.feature.logs.domain.LogsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDateTime

/** Each read takes the first queued answer in [pages]. Queue one before the call. */
class FakeLogsRepository : LogsRepository {
    val pages = ArrayDeque<CompletableDeferred<Result<LogPage>>>()
    val calls = mutableListOf<String?>()

    override suspend fun getLogs(path: String?): Result<LogPage> {
        calls += path
        return pages.first().await()
    }
}

internal fun log(id: String) = LogEntry(
    id = id,
    action = LogAction.Create,
    objectType = LogObjectType.Item,
    objectLabel = "Label $id",
    loggedAt = LocalDateTime(2026, 11, 25, 12, 0)
)
