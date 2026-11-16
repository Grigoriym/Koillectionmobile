package com.grappim.koillectionmobile.core.photos

import android.content.Context
import android.net.Uri
import com.grappim.kit.logger.logcat
import com.grappim.koillectionmobile.core.image.ImagePreparer
import com.grappim.koillectionmobile.core.image.Rotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
actual class PlatformPhotoModule {

    @Single
    fun providePhotoFiles(context: Context): PhotoFiles = AndroidPhotoFiles(context)
}

/**
 * The photos are this app's own `FileProvider` files in `cacheDir/photos/` (`PhotoSources.android.kt`),
 * so the content resolver can delete them.
 */
private class AndroidPhotoFiles(private val context: Context) : PhotoFiles {

    override suspend fun prepare(uri: String, rotation: Rotation): ByteArray = withContext(Dispatchers.IO) {
        ImagePreparer(context.contentResolver).prepare(Uri.parse(uri), rotation)
    }

    override fun delete(uris: List<String>) {
        uris.forEach { uri ->
            runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) }
                .onFailure { logcat(tag = TAG) { "Delete of $uri failed: $it" } }
        }
    }

    private companion object {
        const val TAG = "PhotoFiles"
    }
}
