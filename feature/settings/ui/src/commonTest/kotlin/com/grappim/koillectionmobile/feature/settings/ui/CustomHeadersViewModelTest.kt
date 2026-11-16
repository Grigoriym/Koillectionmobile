package com.grappim.koillectionmobile.feature.settings.ui

import com.grappim.kit.customheaders.CustomHeader
import com.grappim.kit.customheaders.CustomHeaderError
import com.grappim.kit.customheaders.CustomHeaderValidator
import com.grappim.kit.testing.FakeCustomHeadersStorage
import com.grappim.kit.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomHeadersViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    /** The app's reserved names: `NetworkModule.provideCustomHeaderValidator`. */
    private fun viewModel(storage: FakeCustomHeadersStorage) = CustomHeadersViewModel(
        customHeadersStorage = storage,
        customHeaderValidator = CustomHeaderValidator(CustomHeaderValidator.DEFAULT_RESERVED_NAMES + "Authorization")
    )

    @Test
    fun `stored headers are offered back as rows`() = runTest {
        val viewModel = viewModel(FakeCustomHeadersStorage(listOf(TOKEN)))

        assertEquals(listOf(CustomHeaderRow(TOKEN.name, TOKEN.value)), viewModel.uiState.value.rows)
    }

    @Test
    fun `nothing changed means nothing to save`() = runTest {
        val viewModel = viewModel(FakeCustomHeadersStorage(listOf(TOKEN)))

        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `a new header is saved trimmed and blank rows are dropped`() = runTest {
        val storage = FakeCustomHeadersStorage()
        val viewModel = viewModel(storage)
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(0, " X-Proxy-Token ")
        state().onValueChange(0, " s3cret ")
        state().onAdd()
        assertTrue(state().isSaveEnabled)
        state().onSaveClick()

        assertEquals(listOf(listOf(TOKEN)), storage.setHeadersCalledWith)
        assertEquals(listOf(CustomHeaderRow(TOKEN.name, TOKEN.value)), state().rows)
        assertFalse(state().isSaveEnabled)
    }

    @Test
    fun `removing every header saves an empty list`() = runTest {
        val storage = FakeCustomHeadersStorage(listOf(TOKEN))
        val viewModel = viewModel(storage)

        viewModel.uiState.value.onRemove(0)
        viewModel.uiState.value.onSaveClick()

        assertEquals(listOf(emptyList()), storage.setHeadersCalledWith)
        assertTrue(viewModel.uiState.value.rows.isEmpty())
    }

    @Test
    fun `Authorization is reserved because the app sends its own bearer token`() = runTest {
        val storage = FakeCustomHeadersStorage()
        val viewModel = viewModel(storage)
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(0, "authorization")
        state().onValueChange(0, "Basic dXNlcjpwYXNz")

        assertEquals(CustomHeaderError.ReservedName, state().rows.single().error)
        assertFalse(state().isSaveEnabled)
        state().onSaveClick()
        assertTrue(storage.setHeadersCalledWith.isEmpty())
    }

    @Test
    fun `a duplicate name marks the second row`() = runTest {
        val viewModel = viewModel(FakeCustomHeadersStorage(listOf(TOKEN)))
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(1, "x-proxy-token")

        assertEquals(listOf(null, CustomHeaderError.DuplicateName), state().rows.map { it.error })
    }

    @Test
    fun `an edit in progress survives a storage emission`() = runTest {
        val storage = FakeCustomHeadersStorage(listOf(TOKEN))
        val viewModel = viewModel(storage)

        viewModel.uiState.value.onValueChange(0, "changed")
        storage.setHeaders(listOf(OTHER))

        assertEquals("changed", viewModel.uiState.value.rows.single().value)
        assertEquals(listOf(OTHER), viewModel.uiState.value.saved)
    }

    @Test
    fun `a failed save keeps the rows and Save stays on`() = runTest {
        val storage = FakeCustomHeadersStorage().apply { setHeadersThrows = IllegalStateException("disk full") }
        val viewModel = viewModel(storage)
        val state = { viewModel.uiState.value }

        state().onAdd()
        state().onNameChange(0, TOKEN.name)
        state().onValueChange(0, TOKEN.value)
        state().onSaveClick()

        assertEquals(listOf(listOf(TOKEN)), storage.setHeadersCalledWith)
        assertEquals(listOf(CustomHeaderRow(TOKEN.name, TOKEN.value)), state().rows)
        assertFalse(state().isSaving)
        assertTrue(state().isSaveEnabled)
    }

    private companion object {
        val TOKEN = CustomHeader(name = "X-Proxy-Token", value = "s3cret")
        val OTHER = CustomHeader(name = "X-Other", value = "abc")
    }
}
