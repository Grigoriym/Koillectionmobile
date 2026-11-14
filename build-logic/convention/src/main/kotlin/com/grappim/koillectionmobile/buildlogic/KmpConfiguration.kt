package com.grappim.koillectionmobile.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val JDK_VERSION = 21

/**
 * The single edit point for platform targets.
 *
 * Android is the shipped app; its target is declared by
 * `com.android.kotlin.multiplatform.library` in `KmpLibraryConventionPlugin`. Every KMP module
 * also declares `jvm()`, `iosArm64()` and `iosSimulatorArm64()` (as TaigaMobileNova does, plan
 * §5 decision 1) so `commonMain` stays platform-clean and `jvmTest` is the fast test and
 * coverage target. No iOS or desktop app exists yet.
 */
fun Project.configureKmp() {
    // Coverage comes from `jvmTest`. The Android library plugin creates no host-test task
    // unless a module asks for one, so there is no Android test task to exclude here.
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(JDK_VERSION)
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        jvm()
        iosArm64()
        iosSimulatorArm64()

        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.findLibrary("kotlinx.coroutines.core").get())
                implementation(libs.findLibrary("kotlinx.collections").get())
                implementation(libs.findLibrary("kotlinx.date.time").get())

                // Logging is available everywhere without a per-module declaration.
                implementation(libs.findLibrary("grappim.kit.logger").get())
            }
        }
    }
}
