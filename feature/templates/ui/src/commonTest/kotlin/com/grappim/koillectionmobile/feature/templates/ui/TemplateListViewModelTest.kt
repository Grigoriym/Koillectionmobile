package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.error_proxy_rejected
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_http
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_network
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_unexpected
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateListViewModelTest {

    private val repository = FakeTemplatesRepository()
    private val templateChanges = TemplateChanges()
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
        repository.templates.addLast(CompletableDeferred())
    }

    private fun createSut(): TemplateListViewModel {
        queue()
        return TemplateListViewModel(repository, templateChanges)
    }

    private fun answer(result: Result<List<Template>> = Result.success(listOf(novel, atlas, comic))) {
        repository.templates.removeFirst().complete(result)
    }

    @Test
    fun `the screen shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(1, repository.templateCalls)
    }

    @Test
    fun `templates are sorted by name ignoring case`() {
        val sut = createSut()

        answer()

        assertEquals(listOf(atlas, comic, novel), sut.uiState.value.templates)
    }

    @Test
    fun `no templates is empty with no error`() {
        val sut = createSut()

        answer(Result.success(emptyList()))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.templates.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `a network failure shows its error and no templates`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        assertFalse(sut.uiState.value.isLoading)
        assertEquals(NativeText.Resource(RString.templates_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.templates.isEmpty())
    }

    @Test
    fun `an http failure shows its status`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.Http(status = 500, message = null)))

        assertEquals(NativeText.Arguments(RString.templates_error_http, listOf(500)), sut.uiState.value.error)
    }

    @Test
    fun `a proxy rejection shows the proxy message`() {
        val sut = createSut()

        answer(Result.failure(KoillectionError.ProxyRejection(status = 403)))

        assertEquals(NativeText.Arguments(RString.error_proxy_rejected, listOf(403)), sut.uiState.value.error)
    }

    @Test
    fun `an unexpected failure names the templates`() {
        val sut = createSut()

        answer(Result.failure(IllegalStateException("parse")))

        assertEquals(NativeText.Resource(RString.templates_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))

        queue()
        sut.uiState.value.onRetry()

        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())

        answer(Result.success(listOf(novel)))

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(listOf(novel), sut.uiState.value.templates)
        assertEquals(2, repository.templateCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.templateCalls)
    }

    @Test
    fun `a template change reads the list again`() {
        val sut = createSut()
        answer()

        queue()
        templateChanges.notifyChanged("novel")
        answer(Result.success(listOf(novel)))

        assertEquals(listOf(novel), sut.uiState.value.templates)
        assertEquals(2, repository.templateCalls)
    }

    private val novel = Template(id = "novel", name = "Novel")
    private val atlas = Template(id = "atlas", name = "atlas")
    private val comic = Template(id = "comic", name = "Comic")
}
