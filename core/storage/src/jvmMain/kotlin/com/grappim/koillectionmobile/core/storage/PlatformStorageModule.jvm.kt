package com.grappim.koillectionmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.grappim.kit.storage.NoopSecretCipher
import com.grappim.kit.storage.SecretCipher
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.io.File

/**
 * JVM is the test target, not a shipped app (plan §5.1). It has no key store, so the password
 * and the JWT are stored as plain text in `~/.koillectionmobile/`.
 */
@Module(includes = [StorageModule::class])
@Configuration
actual class PlatformStorageModule {

    /** DataStore creates the directory on the first write, not here. */
    @Single
    fun provideDataStore(): DataStore<Preferences> =
        createDataStore(File(System.getProperty("user.home"), ".koillectionmobile/$DATA_STORE_FILE_NAME").absolutePath)

    @Single
    fun provideSecretCipher(): SecretCipher = NoopSecretCipher()
}
