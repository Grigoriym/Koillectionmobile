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
            implementation(projects.feature.items.domain)
            // The item screen shows the item's tags.
            implementation(projects.feature.tags.domain)
            // `KoillectionError`, to pick the error message.
            implementation(projects.core.domain)
            implementation(projects.strings)
            implementation(libs.grappim.kit.uikit)
            // `AsyncImage`. The network fetcher is wired in `:composeApp`.
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `MainDispatcherRule`, and `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
