package com.grappim.koillectionmobile.composeapp.di

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
        AppModule().module().verify()
    }
}
