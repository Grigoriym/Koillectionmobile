plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.di)
    alias(libs.plugins.koillectionmobile.kmp.network)
    alias(libs.plugins.koillectionmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)

            // Wired now so the build proves they resolve; used from M1 on: dispatchers for the
            // client, `SecretCipher` for the JWT, custom headers for reverse proxies.
            implementation(libs.grappim.kit.coroutines)
            implementation(libs.grappim.kit.storage)
            implementation(libs.grappim.kit.customheaders)

            // `api`: feature `data` modules will inject the configured `Json`.
            api(libs.kotlinx.serialization.json)
        }
    }
}
