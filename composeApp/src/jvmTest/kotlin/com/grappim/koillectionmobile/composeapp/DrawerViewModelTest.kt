package com.grappim.koillectionmobile.composeapp

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Moved here from `CollectionListViewModelTest` with Log out (M11.11). */
class DrawerViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `log out clears the session through the auth repository`() {
        val sut = DrawerViewModel(authRepository)

        sut.logOut()

        assertEquals(1, authRepository.logOutCalls)
    }

    private class FakeAuthRepository : AuthRepository {
        var logOutCalls = 0

        override suspend fun logIn(serverUrl: String, username: String, password: String): LoginResult =
            error("not used")

        override suspend fun logOut() {
            logOutCalls++
        }

        override fun storedServerUrl(): String = error("not used")
    }
}
