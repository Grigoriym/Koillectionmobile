@file:OptIn(ExperimentalForeignApi::class)

package com.grappim.koillectionmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.grappim.kit.storage.NoopSecretCipher
import com.grappim.kit.storage.SecretCipher
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS is not shipped (plan §5.1). `NoopSecretCipher` stores the password and the JWT as plain
 * text, so a Keychain-backed `SecretCipher` must replace it before an iOS app exists.
 */
@Module(includes = [StorageModule::class])
@Configuration
actual class PlatformStorageModule {

    @Single
    fun provideDataStore(): DataStore<Preferences> {
        val dir = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return createDataStore(requireNotNull(dir?.path) + "/" + DATA_STORE_FILE_NAME)
    }

    @Single
    fun provideSecretCipher(): SecretCipher = NoopSecretCipher()
}
