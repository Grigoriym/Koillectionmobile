plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
    // `SettingsRoute` is a `@Serializable` `NavKey`: the shell saves it in the back stack.
    alias(libs.plugins.koillectionmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // No `feature:settings:domain`: the ViewModel takes `CustomHeadersStorage` straight,
            // one call on one storage seam (wallosmobile's `CustomHeadersViewModel`).
            implementation(libs.grappim.kit.customheaders)
            // `resultOf` and `logcat` around the save.
            implementation(libs.grappim.kit.domain)
            implementation(libs.grappim.kit.logger)
            implementation(projects.strings)
            implementation(libs.grappim.kit.uikit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `MainDispatcherRule`, `FakeCustomHeadersStorage`, and `runTest` through its
            // kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
