package com.grappim.koillectionmobile.core.api

import com.grappim.kit.customheaders.CustomHeaderValidator
import com.grappim.kit.customheaders.CustomHeadersStorage
import com.grappim.kit.logger.KitLogger
import com.grappim.koillectionmobile.core.storage.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * `SessionStorage` and `CustomHeadersStorage` come from core:storage's `PlatformStorageModule`.
 */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.core.api")
class NetworkModule {

    /**
     * `ignoreUnknownKeys`: the DTOs read a small subset of each resource (plan §2), and a
     * newer server can add fields.
     */
    @Single
    fun provideBaseUrlProvider(sessionStorage: SessionStorage): BaseUrlProvider = object : BaseUrlProvider {
        override fun getBaseUrl(): String = sessionStorage.serverUrl
    }

    /**
     * The client sets `Authorization: Bearer` itself ([bearerAuth]). The `CustomHeaders` plugin
     * runs after it and would replace the token, so a custom header may not use that name.
     */
    @Single
    fun provideCustomHeaderValidator(): CustomHeaderValidator =
        CustomHeaderValidator(CustomHeaderValidator.DEFAULT_RESERVED_NAMES + HttpHeaders.Authorization)

    @Single
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * The engine is the platform default that Ktor finds on the classpath (OkHttp on Android
     * and JVM, Darwin on iOS). Plain platform TLS is enough (plan §5.3).
     *
     * Bodies are logged only when the app installed a logger. Without one, `logcat` drops every
     * line, so reading bodies for it is wasted work.
     */
    @Single
    fun provideHttpClient(
        json: Json,
        baseUrlProvider: BaseUrlProvider,
        sessionStorage: SessionStorage,
        customHeadersStorage: CustomHeadersStorage
    ): HttpClient = HttpClient {
        configureKoillectionClient(
            json = json,
            baseUrlProvider = baseUrlProvider,
            sessionStorage = sessionStorage,
            customHeadersStorage = customHeadersStorage,
            logLevel = if (KitLogger.isInstalled) LogLevel.ALL else LogLevel.NONE
        )
    }
}
