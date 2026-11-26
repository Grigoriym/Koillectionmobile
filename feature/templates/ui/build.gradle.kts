plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
    // The routes are `@Serializable` `NavKey`s: the shell saves them in the back stack.
    alias(libs.plugins.koillectionmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.templates.domain)
            // `KoillectionError`, to pick the error message.
            implementation(projects.core.domain)
            implementation(projects.strings)
            // The shared error message and error view.
            implementation(projects.core.ui)
            implementation(libs.grappim.kit.uikit)
            // `persistentListOf` for the shell top bar's actions.
            implementation(libs.kotlinx.collections)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `MainDispatcherRule`, and `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
