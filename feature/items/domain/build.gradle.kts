plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.stability)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `AddItemUseCase` sets the new item's tags.
            implementation(projects.feature.tags.domain)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
