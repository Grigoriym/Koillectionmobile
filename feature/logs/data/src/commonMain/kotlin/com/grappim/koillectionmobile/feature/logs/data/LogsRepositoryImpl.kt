package com.grappim.koillectionmobile.feature.logs.data

import com.grappim.kit.domain.resultOf
import com.grappim.koillectionmobile.core.api.HydraCollectionDto
import com.grappim.koillectionmobile.feature.logs.domain.LogPage
import com.grappim.koillectionmobile.feature.logs.domain.LogsRepository
import com.grappim.koillectionmobile.feature.logs.dto.LogDto
import com.grappim.koillectionmobile.feature.logs.mapper.LogMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.annotation.Single

/**
 * No cache: every call is a round trip. [LOGS_PATH] is relative to the stored server URL. The
 * `last` and `previous` links are absolute paths that already hold the server's own prefix, so
 * the client requests them as they are (as `getAllHydraPages` does with `next`).
 */
@Single(binds = [LogsRepository::class])
internal class LogsRepositoryImpl(private val httpClient: HttpClient, private val mapper: LogMapper) :
    LogsRepository {

    override suspend fun getLogs(path: String?): Result<LogPage> = resultOf {
        var page = getPage(path ?: LOGS_PATH)
        val last = page.view?.last
        if (path == null && last != null) {
            page = getPage(last)
        }
        LogPage(logs = page.member.asReversed().map(mapper::toDomain), nextPath = page.view?.previous)
    }

    private suspend fun getPage(path: String): HydraCollectionDto<LogDto> = httpClient.get(path).body()
}

private const val LOGS_PATH = "api/logs"
