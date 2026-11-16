import com.android.build.api.dsl.ApplicationExtension
import com.grappim.koillectionmobile.buildlogic.AppBuildTypes
import com.grappim.koillectionmobile.buildlogic.configureComposeStabilityConfig
import com.grappim.koillectionmobile.buildlogic.configureComposeStabilityReports
import com.grappim.koillectionmobile.buildlogic.configureFlavors
import com.grappim.koillectionmobile.buildlogic.configureKotlinAndroid
import com.grappim.koillectionmobile.buildlogic.configureLinting
import com.grappim.koillectionmobile.buildlogic.configureTests
import com.grappim.koillectionmobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "io.insert-koin.compiler.plugin")

            configureComposeStabilityReports()
            configureComposeStabilityConfig()

            extensions.configure<ApplicationExtension> {
                defaultConfig.apply {
                    targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                configureAppBuildTypes()

                bundle {
                    language {
                        enableSplit = false
                    }
                }

                packaging.resources.excludes.apply {
                    add("META-INF/ASL2.0")
                    add("META-INF/notice.txt")
                    add("META-INF/NOTICE.txt")
                    add("META-INF/NOTICE")
                    add("META-INF/license.txt")
                    add("DEPENDENCIES")
                }

                buildFeatures.apply {
                    compose = true
                }

                // Release signing per store flavor arrives with the release milestone (M9).
                configureFlavors(this)
                configureKotlinAndroid(this)
            }

            // `:androidApp` holds MainActivity and the Koin startup glue — real Kotlin that
            // the gates have to cover, even though it is not a KMP module.
            configureTests()
            configureLinting()
        }
    }
}

private fun ApplicationExtension.configureAppBuildTypes() {
    buildTypes {
        debug {
            applicationIdSuffix = AppBuildTypes.DEBUG.applicationIdSuffix

            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            applicationIdSuffix = AppBuildTypes.RELEASE.applicationIdSuffix

            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
