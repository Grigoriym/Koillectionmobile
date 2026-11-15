plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: `SecretCipher` and `CustomHeadersStorage` are bound here and injected in
            // core:api.
            api(libs.grappim.kit.storage)
            api(libs.grappim.kit.customheaders)
            implementation(libs.androidx.datastore.preferences.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            // `FakeSecretCipher`, and `runTest` through its kotlinx-coroutines-test.
            implementation(libs.grappim.kit.testing)
        }
    }
}
