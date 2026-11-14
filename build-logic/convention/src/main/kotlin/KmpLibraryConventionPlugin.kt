import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.grappim.koillectionmobile.buildlogic.configureKmp
import com.grappim.koillectionmobile.buildlogic.configureLinting
import com.grappim.koillectionmobile.buildlogic.configureTests
import com.grappim.koillectionmobile.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // KMP must be applied first so the android plugin can hook into it
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            // The android DSL lives as a sub-extension of the kotlin extension
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            (kotlinExt as ExtensionAware).extensions
                .configure<KotlinMultiplatformAndroidLibraryExtension> {
                    compileSdk = libs.findVersion("compileSdk").get().toString().toInt()
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    namespace = "com.grappim.koillectionmobile" + path.replace(':', '.').replace("-", "")
                }

            configureKmp()
            configureTests()
            configureLinting()
        }
    }
}
