package com.grappim.koillectionmobile.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory `DataStore<Preferences>`. The real one needs a file path, and `commonTest` has no
 * portable way to make one (wallosmobile's fake).
 */
internal class FakePreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state.asStateFlow()

    val current: Preferences
        get() = state.value

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(state.value).also { state.value = it }
    }
}
