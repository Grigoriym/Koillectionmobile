package com.grappim.koillectionmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.grappim.kit.customheaders.CustomHeadersStorage
import com.grappim.kit.customheaders.CustomHeadersStorageImpl
import com.grappim.kit.storage.SecretCipher
import okio.Path.Companion.toPath
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * The platform-free bindings. [PlatformStorageModule] supplies the `DataStore<Preferences>` and
 * the [SecretCipher], and includes this module.
 *
 * One DataStore file and one cipher hold both the session and the custom headers.
 */
@Module
class StorageModule {

    @Single
    fun provideSessionStorage(dataStore: DataStore<Preferences>, secretCipher: SecretCipher): SessionStorage =
        SessionStorageImpl(dataStore, secretCipher)

    @Single
    fun provideCustomHeadersStorage(
        dataStore: DataStore<Preferences>,
        secretCipher: SecretCipher
    ): CustomHeadersStorage = CustomHeadersStorageImpl(dataStore, secretCipher)
}

/**
 * Each platform's module: the DataStore file location and the [SecretCipher]. The app's
 * `AppModule` includes it; it includes [StorageModule].
 */
expect class PlatformStorageModule

internal const val DATA_STORE_FILE_NAME = "koillection_session.preferences_pb"

internal fun createDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })
