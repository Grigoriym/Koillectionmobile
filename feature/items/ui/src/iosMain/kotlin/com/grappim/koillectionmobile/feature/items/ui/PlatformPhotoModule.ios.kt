package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.core.image.Rotation
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** No photo source on this platform (`rememberPhotoSources` is null), so the form never holds a photo. */
@Module
@Configuration
actual class PlatformPhotoModule {

    @Single
    fun providePhotoFiles(): PhotoFiles = NoPhotoFiles
}

private object NoPhotoFiles : PhotoFiles {
    override suspend fun prepare(uri: String, rotation: Rotation): ByteArray =
        throw UnsupportedOperationException("No photos on this platform")

    override fun delete(uris: List<String>) = Unit
}
