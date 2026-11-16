plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `Rotation`; `ImagePreparer` for the upload bytes and the preview on Android.
            api(projects.core.image)
        }
        androidMain.dependencies {
            // The camera and photo picker launchers.
            implementation(libs.androidx.activity.compose)
        }
    }
}
