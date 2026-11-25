plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Both `api`: they are the two ends of the mapper's public signature.
            api(projects.feature.inventories.dto)
            api(projects.feature.inventories.domain)
        }
        // `jvmTest`: the tests read the M12.1 fixture files.
        jvmTest.dependencies {
            implementation(kotlin("test"))
            // Decodes the fixtures. `dto` takes it as `implementation` only.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
