plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.collections.data)
            implementation(projects.feature.collections.mapper)
            implementation(projects.feature.items.data)
            implementation(projects.feature.items.mapper)
            implementation(projects.feature.tags.data)
            implementation(projects.feature.tags.mapper)
        }
        // `KoinGraphTest` uses `verify()`, which koin-test ships for the JVM only.
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
            // `HttpClientEngine` in `KoinGraphTest`'s extra types.
            implementation(libs.ktor.core)
            // The four repositories resolved in `KoinGraphTest`.
            implementation(projects.feature.auth.domain)
            implementation(projects.feature.collections.domain)
            implementation(projects.feature.items.domain)
            implementation(projects.feature.tags.domain)
        }
    }
}
