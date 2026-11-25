plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
}

// Test-only fakes shared by several modules. Take it as a test dependency, never in a main
// source set.
kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: tests that use `FakeSessionStorage` also name `SessionStorage` and
            // `Credentials`.
            api(projects.core.storage)
        }
    }
}
