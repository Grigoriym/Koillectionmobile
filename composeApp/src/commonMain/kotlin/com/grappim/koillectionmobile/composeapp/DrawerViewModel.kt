package com.grappim.koillectionmobile.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

/**
 * The drawer's Log out. It does not navigate: clearing the session turns
 * `SessionStorage.isLoggedIn` `false`, and `KoillectionAppContent` swaps to the login screen. The
 * ViewModel scope, and not the shell's coroutine scope, runs it: the swap disposes the shell.
 *
 * Log out also empties the image loader's memory and disk caches (M14.14, MASVS-PRIVACY-4), so the
 * server's images do not stay on the device for the next login.
 */
@KoinViewModel
class DrawerViewModel(private val authRepository: AuthRepository, private val imageLoader: ImageLoader) : ViewModel() {

    fun logOut() {
        viewModelScope.launch {
            authRepository.logOut()
            imageLoader.memoryCache?.clear()
            withContext(Dispatchers.IO) { imageLoader.diskCache?.clear() }
        }
    }
}
