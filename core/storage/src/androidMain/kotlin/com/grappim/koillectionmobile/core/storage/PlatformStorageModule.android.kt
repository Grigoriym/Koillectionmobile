package com.grappim.koillectionmobile.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.grappim.kit.storage.KeystoreSecretCipher
import com.grappim.kit.storage.SecretCipher
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

private const val KEYSTORE_ALIAS = "koillection_session_key"

/** The shipped platform. The password and the JWT are encrypted with an Android Keystore key. */
@Module(includes = [StorageModule::class])
@Configuration
actual class PlatformStorageModule {

    /** The file is where `preferencesDataStoreFile` would put it: `files/datastore/`. */
    @Single
    fun provideDataStore(context: Context): DataStore<Preferences> =
        createDataStore(context.filesDir.resolve("datastore/$DATA_STORE_FILE_NAME").absolutePath)

    @Single
    fun provideSecretCipher(): SecretCipher = KeystoreSecretCipher(keyAlias = KEYSTORE_ALIAS)
}
