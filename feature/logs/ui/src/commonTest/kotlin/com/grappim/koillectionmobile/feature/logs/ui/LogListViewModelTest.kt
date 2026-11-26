package com.grappim.koillectionmobile.feature.logs.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.logs.domain.LogPage
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_http
import com.grappim.koillectionmobile.strings.generated.resources.error_network
import com.grappim.koillectionmobile.strings.generated.resources.logs_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogListViewModelTest {

    private val repository = FakeLogsRepository()
    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    private fun queue() {
        repository.pages.addLast(CompletableDeferred())
    }

    private fun createSut(): LogListViewModel {
        queue()
        return LogListViewModel(repository)
    }

    private fun answer(result: Result<LogPage>) {
        repository.pages.removeFirst().complete(result)
    }

    private fun page(vararg ids: String, next: String?) = Result.success(LogPage(ids.map(::log), next))

    /** A loaded first page `5 4 3` with `/api/logs?page=34` next. */
    private fun loadedSut(): LogListViewModel {
        val sut = createSut()
        answer(page("5", "4", "3", next = PAGE_34))
        return sut
    }

    @Test
    fun `the first page shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer(page("5", "4", "3", next = PAGE_34))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error.isEmpty())
        assertEquals(listOf("5", "4", "3"), state.logs.map { it.id })
        assertEquals(listOf<String?>(null), repository.calls)
    }

    @Test
    fun `the next page reads the path of the page before and adds its rows at the end`() {
        val sut = loadedSut()
        queue()

        sut.uiState.value.onLoadMore()
        assertTrue(sut.uiState.value.isLoadingMore)
        answer(page("2", "1", next = PAGE_33))

        val state = sut.uiState.value
        assertFalse(state.isLoadingMore)
        assertEquals(listOf("5", "4", "3", "2", "1"), state.logs.map { it.id })
        assertEquals(listOf(null, PAGE_34), repository.calls)

        queue()
        sut.uiState.value.onLoadMore()
        assertEquals(listOf(null, PAGE_34, PAGE_33), repository.calls)
    }

    @Test
    fun `a load more while a page loads makes no second call`() {
        val sut = loadedSut()
        queue()

        sut.uiState.value.onLoadMore()
        sut.uiState.value.onLoadMore()

        assertEquals(listOf(null, PAGE_34), repository.calls)
    }

    @Test
    fun `a load more before the first page arrives makes no call`() {
        val sut = createSut()

        sut.uiState.value.onLoadMore()

        assertEquals(listOf<String?>(null), repository.calls)
    }

    @Test
    fun `after the oldest page a load more makes no call`() {
        val sut = loadedSut()
        queue()
        sut.uiState.value.onLoadMore()
        answer(page("2", "1", next = null))

        sut.uiState.value.onLoadMore()

        assertEquals(listOf(null, PAGE_34), repository.calls)
        assertFalse(sut.uiState.value.isLoadingMore)
    }

    @Test
    fun `a row whose id the list already shows is dropped`() {
        val sut = loadedSut()
        queue()

        sut.uiState.value.onLoadMore()
        answer(page("3", "2", "1", next = null))

        assertEquals(listOf("5", "4", "3", "2", "1"), sut.uiState.value.logs.map { it.id })
    }

    @Test
    fun `an empty first page shows the empty state`() {
        val sut = createSut()

        answer(page(next = null))

        val state = sut.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error.isEmpty())
        assertTrue(state.logs.isEmpty())
    }

    @Test
    fun `a failed first page shows the error and retry reads it again`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(RuntimeException("down"))))

        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)
        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.logs.isEmpty())

        queue()
        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        answer(page("5", next = null))

        assertEquals(listOf("5"), sut.uiState.value.logs.map { it.id })
        assertEquals(listOf<String?>(null, null), repository.calls)
    }

    @Test
    fun `a failed first page makes load more do nothing`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Http(404, null)))

        sut.uiState.value.onLoadMore()

        assertEquals(listOf<String?>(null), repository.calls)
        assertEquals(NativeText.Arguments(RString.error_http, listOf(404)), sut.uiState.value.error)
    }

    @Test
    fun `a failed later page keeps the rows and retry more reads the same page`() {
        val sut = loadedSut()
        queue()
        sut.uiState.value.onLoadMore()
        answer(Result.failure(IllegalStateException("boom")))

        val failed = sut.uiState.value
        assertFalse(failed.isLoadingMore)
        assertEquals(NativeText.Resource(RString.logs_error_unexpected), failed.moreError)
        assertEquals(listOf("5", "4", "3"), failed.logs.map { it.id })

        // A scroll does not retry on its own.
        failed.onLoadMore()
        assertEquals(listOf(null, PAGE_34), repository.calls)

        queue()
        failed.onRetryMore()
        assertTrue(sut.uiState.value.isLoadingMore)
        assertTrue(sut.uiState.value.moreError.isEmpty())
        answer(page("2", next = null))

        assertEquals(listOf(null, PAGE_34, PAGE_34), repository.calls)
        assertEquals(listOf("5", "4", "3", "2"), sut.uiState.value.logs.map { it.id })
    }

    private companion object {
        const val PAGE_34 = "/api/logs?page=34"
        const val PAGE_33 = "/api/logs?page=33"
    }
}
