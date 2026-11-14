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
            implementation(projects.feature.auth.mapper)
        }
    }
}
