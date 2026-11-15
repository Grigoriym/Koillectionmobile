package com.grappim.koillectionmobile.composeapp.di

import io.ktor.client.engine.HttpClientEngine
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
 * constructor (deskmate's M2.4 finding). Only a launch of the app catches that case.
 */
class KoinGraphTest {
    @Test
    fun `every definition in the app graph can be resolved`() {
        AppModule().module().verify(extraTypes = EXTERNALLY_SUPPLIED)
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
