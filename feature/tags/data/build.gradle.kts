plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
    alias(libs.plugins.koillectionmobile.kmp.network)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.core.domain)

            // `dto` and `domain` arrive through the mapper, which exposes both as `api`.
            implementation(projects.feature.tags.mapper)
        }
        // `jvmTest`: the tests serve the M1.1 fixture files.
        jvmTest.dependencies {
            implementation(kotlin("test"))
            // `FakeSessionStorage`.
            implementation(projects.core.testing)
            implementation(libs.ktor.client.mock)
            // `LogLevel` in `configureKoillectionClient`'s signature.
            implementation(libs.ktor.client.logging)
            // `FakeCustomHeadersStorage`, and `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
