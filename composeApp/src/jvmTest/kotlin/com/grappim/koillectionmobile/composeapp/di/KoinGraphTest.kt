package com.grappim.koillectionmobile.composeapp.di

import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * A missing Koin definition is otherwise a launch-time crash that no gate catches: a
 * `@ComponentScan` that misses a class, or an `AppModule` that forgets an `includes` line, both
 * compile.
 *
 * `verify()` checks every definition's constructor by reflection and fails on a parameter type
 * the module set can't supply, without instantiating anything. It is JVM-only, so this test
 * lives in `jvmTest`, not `commonTest`.
 *
 * Limit: it does not check a provider function's own parameters, only the bound type's
 * constructor (deskmate's M2.4 finding). A provider that returns an interface passes with any
 * parameter unbound. The second test resolves the real JVM graph to cover that.
 */
class KoinGraphTest {
    @Test
    fun `every definition in the app graph can be resolved`() {
        AppModule().module().verify(extraTypes = EXTERNALLY_SUPPLIED)
    }

    /**
     * Covers the limit above for the provider chain `verify()` cannot see: `HttpClient` needs
     * `BaseUrlProvider`, `SessionStorage` and `CustomHeadersStorage`, all from provider functions.
     * `AuthRepository` is resolved too: `verify()` checks only what the graph defines, so a
     * repository without its `@Single` would not fail it (mutation-checked in M1.5). A missing
     * `includes` line still passes here, as in M1.3. Resolving them creates no file: DataStore
     * writes only on the first save.
     */
    @Test
    fun `the HTTP client and the repositories resolve from the JVM app graph`() {
        val koin = startKoin<KoinApp> { }.koin
        try {
            koin.get<AuthRepository>()
            koin.get<HttpClient>().close()
        } finally {
            stopKoin()
        }
    }

    private companion object {
        /**
         * Types nothing in `AppModule` defines, on purpose:
         * - `HttpClientEngine` is a false positive. `verify()` reads `NetworkModule`'s
         *   `provideHttpClient` through the `HttpClient(engine)` constructor, not through the
         *   function's own parameters. The function builds the client on Ktor's default engine,
         *   so Koin is never asked for one (wallosmobile has the same entry).
         */
        val EXTERNALLY_SUPPLIED = listOf(HttpClientEngine::class)
    }
}
