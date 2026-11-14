plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.tags.domain)
            implementation(projects.strings)
            implementation(libs.grappim.kit.uikit)
        }
    }
}
