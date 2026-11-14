plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.stability)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: `resultOf`/`mapResult` and the cert-trust types reach every module through
            // core:domain, as in wallosmobile.
            api(libs.grappim.kit.domain)
        }
    }
}
