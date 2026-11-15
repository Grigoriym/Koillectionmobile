plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.stability)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: `LoginResult.Failed` carries a `KoillectionError`.
            api(projects.core.domain)
        }
    }
}
