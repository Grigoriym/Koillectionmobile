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
            // `PlatformPhotoModule` in `AppModule`.
            implementation(projects.core.photos)
            // The local network banner's text.
            implementation(projects.strings)
            implementation(libs.grappim.kit.uikit)
            // The drawer shell: one back stack per drawer section.
            implementation(libs.grappim.kit.navigation)
            // `persistentListOf` for the drawer items.
            implementation(libs.kotlinx.collections)
            // Log out is a drawer action (`DrawerViewModel`).
            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.ui)
            implementation(projects.feature.collections.ui)
            implementation(projects.feature.items.ui)
            implementation(projects.feature.settings.ui)
            implementation(projects.feature.tags.ui)
            implementation(projects.feature.templates.ui)
            implementation(projects.feature.wishlists.ui)
            implementation(projects.feature.loans.ui)
            implementation(projects.feature.albums.ui)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.collections.data)
            implementation(projects.feature.collections.mapper)
            implementation(projects.feature.items.data)
            implementation(projects.feature.items.mapper)
            implementation(projects.feature.tags.data)
            implementation(projects.feature.tags.mapper)
            implementation(projects.feature.templates.data)
            implementation(projects.feature.templates.mapper)
            implementation(projects.feature.wishlists.data)
            implementation(projects.feature.wishlists.mapper)
            implementation(projects.feature.loans.data)
            implementation(projects.feature.loans.mapper)
            implementation(projects.feature.albums.data)
            implementation(projects.feature.albums.mapper)

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
            // `MainDispatcherRule` in `DrawerViewModelTest`.
            implementation(libs.grappim.kit.testing)
            // The repositories resolved in `KoinGraphTest`.
            implementation(projects.feature.collections.domain)
            implementation(projects.feature.items.domain)
            implementation(projects.feature.tags.domain)
            implementation(projects.feature.templates.domain)
            implementation(projects.feature.wishlists.domain)
            implementation(projects.feature.loans.domain)
            implementation(projects.feature.albums.domain)
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

// M3.1, M5.2: live write smoke run against the local test instance. It creates an item in `Books`,
// edits it and deletes it again. Same setup as `smokeRun`; never point it at the real server.
tasks.register<JavaExec>("smokeWriteRun") {
    group = "verification"
    description = "Creates, edits and deletes one item on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveWriteSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M6.2: live collection smoke run against the local test instance. It creates a collection and a
// child, edits the child and deletes the parent. Same setup as `smokeRun`; never point it at the
// real server.
tasks.register<JavaExec>("smokeCollectionRun") {
    group = "verification"
    description = "Creates, edits and deletes collections on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveCollectionSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M7.4: live tag smoke run against the local test instance. It creates a tag, puts it on an item
// that has tags, takes it off again and deletes it. Same setup as `smokeRun`; never point it at the
// real server.
tasks.register<JavaExec>("smokeTagRun") {
    group = "verification"
    description = "Creates, assigns, removes and deletes a tag on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveTagSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M8.2: live template smoke run against the local test instance. It creates a choice list, a
// template with typed fields, a collection with it as the default template and an item with typed
// fields, then deletes them all. Same setup as `smokeRun`; never point it at the real server.
tasks.register<JavaExec>("smokeTemplateRun") {
    group = "verification"
    description = "Creates and deletes a template, a choice list and typed item fields on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveTemplateSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M9.2: live wishlist smoke run against the local test instance. It creates a wishlist, a child and
// a wish with images, edits them and deletes the parent. Same setup as `smokeRun`; never point it at
// the real server.
tasks.register<JavaExec>("smokeWishlistRun") {
    group = "verification"
    description = "Creates, edits and deletes wishlists and a wish on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveWishlistSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M10.2: live loan smoke run against the local test instance. It creates an item with two loans,
// edits one, sends two invalid creates and deletes them all. Same setup as `smokeRun`; never point
// it at the real server.
tasks.register<JavaExec>("smokeLoanRun") {
    group = "verification"
    description = "Creates, edits and deletes loans on a throwaway item on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveLoanSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

// M11.2: live album smoke run against the local test instance. It creates an album, a child and a
// photo with images, edits them and deletes the parent. Same setup as `smokeRun`; never point it at
// the real server.
tasks.register<JavaExec>("smokeAlbumRun") {
    group = "verification"
    description = "Creates, edits and deletes albums and a photo on the local test instance."
    val jvmTestCompilation = kotlin.jvm().compilations.getByName("test")
    dependsOn(jvmTestCompilation.compileAllTaskName)
    classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    mainClass.set("com.grappim.koillectionmobile.composeapp.smoke.LiveAlbumSmokeKt")
    val smokeHome = layout.buildDirectory.dir("smoke-home")
    systemProperty("user.home", smokeHome.get().asFile.absolutePath)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
