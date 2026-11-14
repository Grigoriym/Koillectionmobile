plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        // `KoinGraphTest` uses `verify()`, which koin-test ships for the JVM only.
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }
    }
}
