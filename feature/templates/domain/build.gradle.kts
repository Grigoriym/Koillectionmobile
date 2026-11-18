plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.stability)
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
