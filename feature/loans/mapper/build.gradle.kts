plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Both `api`: they are the two ends of the mapper's public signature.
            api(projects.feature.loans.dto)
            api(projects.feature.loans.domain)
        }
        // `jvmTest`: the date-part tests.
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
