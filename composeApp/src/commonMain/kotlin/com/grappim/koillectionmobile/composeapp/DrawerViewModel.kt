package com.grappim.koillectionmobile.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The drawer's Log out. It does not navigate: clearing the session turns
 * `SessionStorage.isLoggedIn` `false`, and `KoillectionAppContent` swaps to the login screen. The
 * ViewModel scope, and not the shell's coroutine scope, runs it: the swap disposes the shell.
 */
@KoinViewModel
class DrawerViewModel(private val authRepository: AuthRepository) : ViewModel() {

    fun logOut() {
        viewModelScope.launch { authRepository.logOut() }
    }
}
