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
            // The choice lists of the `choice-list` fields.
            implementation(projects.feature.templates.domain)
            // The add form reads its collection's default template.
            implementation(projects.feature.collections.domain)
            // `KoillectionError`, to pick the error message.
            implementation(projects.core.domain)
            // `Rotation` in the form state.
            implementation(projects.core.image)
            // The camera, the picker, the photo preview and `PhotoFiles`.
            implementation(projects.core.photos)
            implementation(projects.strings)
            // The shared error message and error view.
            implementation(projects.core.ui)
            implementation(libs.grappim.kit.uikit)
            // `persistentListOf` for the shell top bar's actions.
            implementation(libs.kotlinx.collections)
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
