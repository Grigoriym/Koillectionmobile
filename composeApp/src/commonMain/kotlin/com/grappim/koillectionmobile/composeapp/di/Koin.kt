package com.grappim.koillectionmobile.composeapp.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/**
 * The whole graph, listed once. A `@Module` in another Gradle module is invisible to this
 * compilation's `@Configuration` discovery, so each feature's module class goes into `includes`
 * here as it appears. Nothing is defined yet: the feature modules are still placeholders (M0.3).
 *
 * The `@ComponentScan` picks up this module's own definitions.
 */
@Module
@Configuration
@ComponentScan("com.grappim.koillectionmobile.composeapp")
class AppModule

@KoinApplication
object KoinApp
