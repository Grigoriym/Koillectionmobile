plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
    alias(libs.plugins.koillectionmobile.kmp.network)
    alias(libs.plugins.koillectionmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: `KoillectionError` is what every caller of the client catches.
            api(projects.core.domain)

            // `api`: `SessionStorage` is in `configureKoillectionClient`'s signature.
            api(projects.core.storage)

            // Wired now so the build proves it resolves; for dispatchers.
            implementation(libs.grappim.kit.coroutines)
            // `api`: `CustomHeadersStorage` is in `configureKoillectionClient`'s signature.
            api(libs.grappim.kit.customheaders)

            // `api`: feature `data` modules will inject the configured `Json`.
            api(libs.kotlinx.serialization.json)

            // Only this module installs these plugins. Feature modules call `body<T>()`, which
            // is in ktor-client-core.
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `FakeSessionStorage`.
            implementation(projects.core.testing)
            implementation(libs.ktor.client.mock)
            // `FakeCustomHeadersStorage`, and `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
