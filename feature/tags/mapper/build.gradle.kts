plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Both `api`: they are the two ends of the mapper's public signature.
            api(projects.feature.tags.dto)
            api(projects.feature.tags.domain)
        }
        // `jvmTest`: the tests read the M1.1 fixture files.
        jvmTest.dependencies {
            implementation(kotlin("test"))
            // Decodes the fixtures. `dto` takes it as `implementation` only.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
