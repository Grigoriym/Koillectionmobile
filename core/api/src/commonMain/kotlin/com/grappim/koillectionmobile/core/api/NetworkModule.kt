package com.grappim.koillectionmobile.core.api

import com.grappim.kit.customheaders.CustomHeadersStorage
import com.grappim.kit.logger.KitLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * [BaseUrlProvider] and [CustomHeadersStorage] have no binding yet: both read stored values,
 * and storage arrives in M1.3. Nothing resolves the `HttpClient` before then.
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
        customHeadersStorage: CustomHeadersStorage
    ): HttpClient = HttpClient {
        configureKoillectionClient(
            json = json,
            baseUrlProvider = baseUrlProvider,
            customHeadersStorage = customHeadersStorage,
            logLevel = if (KitLogger.isInstalled) LogLevel.ALL else LogLevel.NONE
        )
    }
}
