package com.grappim.koillectionmobile.composeapp.di

import com.grappim.koillectionmobile.core.api.NetworkModule
import com.grappim.koillectionmobile.core.storage.PlatformStorageModule
import com.grappim.koillectionmobile.feature.auth.data.AuthDataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/**
 * The whole graph, listed once. Each feature's module class goes into `includes` here as it
 * appears. On JVM a `@Configuration` module in another Gradle module is found without this list
 * (M1.3: `KoinApp` still resolved `PlatformStorageModule` with it removed). Keep the list anyway:
 * other platforms are not checked.
 *
 * The `@ComponentScan` picks up this module's own definitions.
 */
@Module(includes = [NetworkModule::class, PlatformStorageModule::class, AuthDataModule::class])
@Configuration
@ComponentScan("com.grappim.koillectionmobile.composeapp")
class AppModule

@KoinApplication
object KoinApp
