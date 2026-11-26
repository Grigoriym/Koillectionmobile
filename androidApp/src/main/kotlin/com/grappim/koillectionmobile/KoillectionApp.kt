package com.grappim.koillectionmobile

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.grappim.koillectionmobile.composeapp.di.KoinApp
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.plugin.module.dsl.startKoin

class KoillectionApp :
    Application(),
    SingletonImageLoader.Factory {

    private val imageLoader: ImageLoader by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin<KoinApp> {
            androidContext(this@KoillectionApp)
        }
    }

    /**
     * Coil asks for its singleton on the first `AsyncImage`, after `onCreate` has started Koin.
     * The loader from `AppModule` sends the user's custom headers with each image request.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
