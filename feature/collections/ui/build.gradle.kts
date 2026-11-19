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
            implementation(projects.feature.collections.domain)
            // The collection screen lists the collection's items too.
            implementation(projects.feature.items.domain)
            // Log out sits in the collection list's top bar.
            implementation(projects.feature.auth.domain)
            // The edit form picks the collection's default template.
            implementation(projects.feature.templates.domain)
            // `KoillectionError`, to pick the error message.
            implementation(projects.core.domain)
            // The collection form's photo: camera, picker, preview and `PhotoFiles`; `Rotation`.
            implementation(projects.core.photos)
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
