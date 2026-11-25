package com.grappim.koillectionmobile.composeapp.di

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import java.io.File
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `KoinGraphTest` passes with an `includes` line missing: on the JVM the Koin plugin finds a
 * `@Configuration` module on its own. Android does not, and the app crashes at launch
 * (`SettingsUiModule`, M4.2). This test reads the annotations by reflection instead.
 *
 * It finds every `@Configuration` class of the app on the test classpath and checks that
 * `AppModule`'s `@Module(includes)` names it. A module that is not on this classpath at all is
 * not found.
 */
class AppModuleIncludesTest {
    @Test
    fun `AppModule includes every Configuration module`() {
        val configurationModules = appClasses()
            .filter { it.isAnnotationPresent(Configuration::class.java) }
            .filter { it != AppModule::class.java }
            .map { it.name }
            .toSet()
        val includes = AppModule::class.java.getAnnotation(Module::class.java)
            .includes
            .map { it.java.name }
            .toSet()

        assertTrue(configurationModules.isNotEmpty(), "No @Configuration class found")
        assertEquals(emptySet(), configurationModules - includes, "Not in AppModule's includes")
    }

    private fun appClasses(): List<Class<*>> = System.getProperty("java.class.path")
        .split(File.pathSeparator)
        .map(::File)
        .flatMap(::classNames)
        .filter { it.startsWith(APP_PACKAGE) }
        .distinct()
        .map { Class.forName(it, false, javaClass.classLoader) }

    private fun classNames(entry: File): List<String> = when {
        entry.isDirectory -> entry.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(entry).invariantSeparatorsPath }
            .toList()

        entry.isFile && entry.extension == "jar" -> JarFile(entry).use { jar ->
            jar.entries().asSequence().map { it.name }.toList()
        }

        else -> emptyList()
    }
        .filter { it.endsWith(".class") && !it.endsWith("module-info.class") }
        .map { it.removeSuffix(".class").replace('/', '.') }

    private companion object {
        const val APP_PACKAGE = "com.grappim.koillectionmobile."
    }
}
