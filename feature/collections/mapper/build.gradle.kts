plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Both `api`: they are the two ends of the mapper's public signature.
            api(projects.feature.collections.dto)
            api(projects.feature.collections.domain)
        }
    }
}
