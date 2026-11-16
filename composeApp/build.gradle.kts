plugins {
    alias(libs.plugins.koillectionmobile.kmp.library)
    alias(libs.plugins.koillectionmobile.kmp.library.compose)
    alias(libs.plugins.koillectionmobile.kmp.di)
    // The polymorphic `SerializersModule` in `nav/NavKeySerializers.kt`.
    alias(libs.plugins.koillectionmobile.kmp.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.api)
            // The local network banner's text.
            implementation(projects.strings)
            implementation(libs.grappim.kit.uikit)
            implementation(projects.feature.auth.ui)
            implementation(projects.feature.collections.ui)
            implementation(projects.feature.items.ui)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.collections.data)
            implementation(projects.feature.collections.mapper)
            implementation(projects.feature.items.data)
            implementation(projects.feature.items.mapper)
            implementation(projects.feature.tags.data)
            implementation(projects.feature.tags.mapper)

            // Coil finds this network fetcher through its service loader. Collection and item
            // images are public files under `uploads/`: they need no token.
            implementation(libs.coil.ktor)
        }
        // `KoinGraphTest` uses `verify()`, which koin-test ships for the JVM only.
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
            // `HttpClientEngine` in `KoinGraphTest`'s extra types.
            implementation(libs.ktor.core)
            // The four repositories resolved in `KoinGraphTest`.
            implementation(projects.feature.auth.domain)
            implementation(projects.feature.collections.domain)
            implementation(projects.feature.items.domain)
            implementation(projects.feature.tags.domain)
        }
    }
}

// M1.11: live read-only smoke run against the local test instance (`docs/local-info.md`). Not
// wired into `build` or `check`; run it by name. `user.home` points into `build/`, so the stored
// session stays out of `~/.koillectionmobile`.
tasks.register<JavaExec>("smokeRun") {
    group = "verification"
    description = "Logs in to the local test instance and prints what the read repositories return."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M3.1: live write smoke run against the local test instance. It creates an item in `Books` and
// deletes it again. Same setup as `smokeRun`; never point it at the real server.
tasks.register<JavaExec>("smokeWriteRun") {
    group = "verification"
    description = "Creates, reads back and deletes one item on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveWriteSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
