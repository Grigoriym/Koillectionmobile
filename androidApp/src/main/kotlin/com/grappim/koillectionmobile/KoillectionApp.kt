package com.grappim.koillectionmobile

import android.app.Application
import com.grappim.koillectionmobile.composeapp.di.KoinApp
import org.koin.android.ext.koin.androidContext
import org.koin.plugin.module.dsl.startKoin

class KoillectionApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin<KoinApp> {
            androidContext(this@KoillectionApp)
        }
    }
}
