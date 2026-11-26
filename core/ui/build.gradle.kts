plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
}

// UI parts that several `ui` modules share and that are specific to this app. Generic widgets
// come from `grappim-kit-uikit` (plan §5 decision 4).
kotlin {
    sourceSets {
        commonMain.dependencies {
            // `KoillectionError`, to pick the error message.
            implementation(projects.core.domain)
            // `api`: the public functions take a `StringResource` and return a `NativeText`.
            api(projects.strings)
            api(libs.grappim.kit.uikit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
