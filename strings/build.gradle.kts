plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: consumers need to resolve `StringResource` themselves.
            api(libs.jetbrains.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.grappim.koillectionmobile.strings.generated.resources"
    generateResClass = always
    publicResClass = true
}
