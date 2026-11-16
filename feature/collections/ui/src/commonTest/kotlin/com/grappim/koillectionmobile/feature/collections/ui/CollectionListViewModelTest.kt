package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.auth.domain.AuthRepository
import com.grappim.koillectionmobile.feature.auth.domain.LoginResult
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collections_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionListViewModelTest {

    private val repository = FakeCollectionsRepository()
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

    private fun errorFor(error: Throwable): NativeText {
        repository.next().complete(Result.failure(error))
        val sut = CollectionListViewModel(repository, authRepository)
        return sut.uiState.value.error
    }

    @Test
    fun `the first load shows loading until the repository answers`() {
        val response = repository.next()
        val sut = CollectionListViewModel(repository, authRepository)
        assertTrue(sut.uiState.value.isLoading)

        response.complete(Result.success(listOf(BOOKS)))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(listOf(BOOKS), sut.uiState.value.collections)
    }

    @Test
    fun `the list keeps only the top-level collections in server order`() {
        repository.next().complete(Result.success(listOf(BOOKS, NOVELS, GAMES)))

        val sut = CollectionListViewModel(repository, authRepository)

        assertEquals(listOf(BOOKS, GAMES), sut.uiState.value.collections)
    }

    @Test
    fun `no collections is an empty list with no error`() {
        repository.next().complete(Result.success(emptyList()))

        val sut = CollectionListViewModel(repository, authRepository)

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.collections.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure has the network message`() {
        assertEquals(
            NativeText.Resource(RString.collections_error_network),
            errorFor(KoillectionError.Network(IllegalStateException("timeout")))
        )
    }

    @Test
    fun `a failure with an HTTP status names the status`() {
        assertEquals(
            NativeText.Arguments(RString.collections_error_http, listOf(502)),
            errorFor(KoillectionError.Server(status = 502, message = null))
        )
        assertEquals(
            NativeText.Arguments(RString.collections_error_http, listOf(403)),
            errorFor(KoillectionError.Http(status = 403, message = null))
        )
    }

    @Test
    fun `a proxy rejection has the proxy message`() {
        assertEquals(
            NativeText.Arguments(RString.error_proxy_rejected, listOf(401)),
            errorFor(KoillectionError.ProxyRejection(status = 401))
        )
    }

    @Test
    fun `any other failure has the unexpected message`() {
        assertEquals(
            NativeText.Resource(RString.collections_error_unexpected),
            errorFor(KoillectionError.Unauthorized(message = null))
        )
    }

    @Test
    fun `refresh loads again and replaces the list`() {
        repository.next().complete(Result.success(listOf(BOOKS)))
        val sut = CollectionListViewModel(repository, authRepository)
        val response = repository.next()

        sut.uiState.value.onRefresh()
        assertTrue(sut.uiState.value.isRefreshing)
        assertEquals(listOf(BOOKS), sut.uiState.value.collections)

        response.complete(Result.success(listOf(BOOKS, GAMES)))

        assertFalse(sut.uiState.value.isRefreshing)
        assertEquals(listOf(BOOKS, GAMES), sut.uiState.value.collections)
        assertEquals(2, repository.calls)
    }

    @Test
    fun `a failed refresh keeps the old list and shows the error`() {
        repository.next().complete(Result.success(listOf(BOOKS)))
        val sut = CollectionListViewModel(repository, authRepository)
        repository.next().complete(Result.failure(KoillectionError.Network(IllegalStateException("offline"))))

        sut.uiState.value.onRefresh()

        assertFalse(sut.uiState.value.isRefreshing)
        assertEquals(listOf(BOOKS), sut.uiState.value.collections)
        assertEquals(NativeText.Resource(RString.collections_error_network), sut.uiState.value.error)
    }

    @Test
    fun `a refresh after an error clears the error on success`() {
        repository.next().complete(Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        val sut = CollectionListViewModel(repository, authRepository)
        repository.next().complete(Result.success(listOf(BOOKS)))

        sut.uiState.value.onRefresh()

        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(BOOKS), sut.uiState.value.collections)
    }

    @Test
    fun `refresh while a load runs sends no second request`() {
        repository.next()
        val sut = CollectionListViewModel(repository, authRepository)

        sut.uiState.value.onRefresh()

        assertEquals(1, repository.calls)
    }

    @Test
    fun `log out clears the session through the auth repository`() {
        repository.next().complete(Result.success(listOf(BOOKS)))
        val sut = CollectionListViewModel(repository, authRepository)

        sut.uiState.value.onLogOutClick()

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

    /** Each call takes the next queued response. Queue one with [next] before the call. */
    private class FakeCollectionsRepository : CollectionsRepository {
        private val responses = ArrayDeque<CompletableDeferred<Result<List<Collection>>>>()
        var calls = 0

        fun next(): CompletableDeferred<Result<List<Collection>>> = CompletableDeferred<Result<List<Collection>>>()
            .also { responses.addLast(it) }

        override suspend fun getCollections(): Result<List<Collection>> {
            calls++
            return responses.removeFirst().await()
        }

        override suspend fun getCollection(id: String): Result<Collection> = error("not used")

        override suspend fun getChildren(id: String): Result<List<Collection>> = error("not used")
    }

    private companion object {
        val BOOKS = Collection(id = "books", title = "Books", parentId = null, color = null, imageUrl = null)
        val NOVELS = Collection(id = "novels", title = "Novels", parentId = "books", color = null, imageUrl = null)
        val GAMES = Collection(
            id = "games",
            title = "Games",
            parentId = null,
            color = null,
            imageUrl = "https://koi.example/uploads/u/games.jpg"
        )
    }
}
