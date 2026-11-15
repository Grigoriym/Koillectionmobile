package com.grappim.koillectionmobile.composeapp.di

import com.grappim.koillectionmobile.core.api.NetworkModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/**
 * The whole graph, listed once. A `@Module` in another Gradle module is invisible to this
 * compilation's `@Configuration` discovery, so each feature's module class goes into `includes`
 * here as it appears.
 *
 * The `@ComponentScan` picks up this module's own definitions.
 */
@Module(includes = [NetworkModule::class])
@Configuration
@ComponentScan("com.grappim.koillectionmobile.composeapp")
class AppModule

@KoinApplication
object KoinApp
