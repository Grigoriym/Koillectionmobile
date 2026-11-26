plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    // `FormPhoto` is `@Serializable`: the forms keep it in their saved draft.
    alias(libs.plugins.koillectionmobile.kmp.serialization)
}

// The name-and-image form that collections, wishlists and albums share: the photo, the save
// status, the frame, the image section, the save view and the save message (M14.11).
kotlin {
    sourceSets {
        commonMain.dependencies {
            // `KoillectionError`, to pick the save message.
            implementation(projects.core.domain)
            // `api`: `FormPhoto` holds a `Rotation`. Camera, picker and preview.
            api(projects.core.photos)
            // `api`: `FormSaveStatus.Failed` holds a `NativeText`.
            api(projects.core.ui)
            // `persistentListOf` for the shell top bar's actions.
            implementation(libs.kotlinx.collections)
            // `AsyncImage`. The network fetcher is wired in `:composeApp`.
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
