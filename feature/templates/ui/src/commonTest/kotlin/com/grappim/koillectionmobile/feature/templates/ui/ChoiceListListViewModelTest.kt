package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceListChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.choice_lists_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.templates_error_network
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChoiceListListViewModelTest {

    private val repository = FakeTemplatesRepository()
    private val choiceListChanges = ChoiceListChanges()
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
        repository.choiceLists.addLast(CompletableDeferred())
    }

    private fun createSut(): ChoiceListListViewModel {
        queue()
        return ChoiceListListViewModel(repository, choiceListChanges)
    }

    private fun answer(result: Result<List<ChoiceList>> = Result.success(listOf(genres, formats, bindings))) {
        repository.choiceLists.removeFirst().complete(result)
    }

    @Test
    fun `the screen shows loading until the answer arrives`() {
        val sut = createSut()
        assertTrue(sut.uiState.value.isLoading)

        answer()

        assertFalse(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(1, repository.choiceListCalls)
    }

    @Test
    fun `choice lists are sorted by name ignoring case`() {
        val sut = createSut()
        answer()

        assertEquals(listOf("bindings", "Formats", "Genres"), sut.uiState.value.choiceLists.map { it.name })
    }

    @Test
    fun `no choice lists is empty with no error`() {
        val sut = createSut()
        answer(Result.success(emptyList()))

        assertTrue(sut.uiState.value.choiceLists.isEmpty())
        assertTrue(sut.uiState.value.error.isEmpty())
    }

    @Test
    fun `an unexpected failure names the choice lists`() {
        val sut = createSut()
        answer(Result.failure(IllegalStateException("boom")))

        assertEquals(NativeText.Resource(RString.choice_lists_error_unexpected), sut.uiState.value.error)
    }

    @Test
    fun `retry after a failure loads again and clears the error`() {
        val sut = createSut()
        answer(Result.failure(KoillectionError.Network(IllegalStateException("timeout"))))
        assertEquals(NativeText.Resource(RString.templates_error_network), sut.uiState.value.error)
        assertTrue(sut.uiState.value.choiceLists.isEmpty())

        queue()
        sut.uiState.value.onRetry()
        assertTrue(sut.uiState.value.isLoading)
        assertTrue(sut.uiState.value.error.isEmpty())
        answer()

        assertEquals(3, sut.uiState.value.choiceLists.size)
        assertEquals(2, repository.choiceListCalls)
    }

    @Test
    fun `retry while loading sends no second call`() {
        val sut = createSut()

        sut.uiState.value.onRetry()

        assertEquals(1, repository.choiceListCalls)
    }

    @Test
    fun `a choice list change reads the list again`() {
        val sut = createSut()
        answer()

        queue()
        choiceListChanges.notifyChanged()
        answer(Result.success(listOf(genres)))

        assertEquals(listOf(genres), sut.uiState.value.choiceLists)
        assertEquals(2, repository.choiceListCalls)
    }

    private val genres = ChoiceList("genres", "Genres", listOf("Fiction", "Poetry"))
    private val formats = ChoiceList("formats", "Formats", listOf("Ebook"))
    private val bindings = ChoiceList("bindings", "bindings", listOf("Hardcover"))
}
