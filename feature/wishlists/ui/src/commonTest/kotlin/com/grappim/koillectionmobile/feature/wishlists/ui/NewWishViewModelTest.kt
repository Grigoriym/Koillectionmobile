package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.lifecycle.SavedStateHandle
import com.grappim.kit.testing.MainDispatcherRule
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChange
import com.grappim.koillectionmobile.feature.wishlists.domain.WishChanges
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_network
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_validation
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewWishViewModelTest {

    private val repository = FakeWishlistsRepository()
    private val photoFiles = FakePhotoFiles()
    private val wishChanges = WishChanges()
    private val mainDispatcherRule = MainDispatcherRule()
    private val changes = mutableListOf<WishChange>()
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        mainDispatcherRule.setup()
        scope = CoroutineScope(Dispatchers.Main)
        scope.launch { wishChanges.changed.collect { changes += it } }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        mainDispatcherRule.tearDown()
    }

    private val savedStateHandle = SavedStateHandle()

    private fun createSut(handle: SavedStateHandle = savedStateHandle) =
        NewWishViewModel(WISHLIST_ID, repository, photoFiles, wishChanges, handle)

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    private fun NewWishViewModel.type(field: WishField, text: String) = uiState.value.onFieldChange(field, text)

    @Test
    fun `a blank name cannot be saved`() {
        val sut = createSut()
        sut.type(WishField.NAME, "  ")
        sut.type(WishField.PRICE, "12")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(sut.uiState.value.hasInput)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a currency that is not three letters cannot be saved`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Tent")
        sut.type(WishField.CURRENCY, "euro")

        sut.uiState.value.onSave()

        assertEquals("EURO", sut.uiState.value.values.currency)
        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a save without an image creates the wish with the filled fields and tells the wishlist`() {
        val sut = createSut()
        sut.type(WishField.NAME, " Tent ")
        sut.type(WishField.PRICE, "12.50")
        sut.type(WishField.CURRENCY, "eur")
        sut.type(WishField.COMMENT, "   ")

        sut.uiState.value.onSave()

        assertEquals(listOf("createWish:travel:Tent:null:12.50:EUR:null"), repository.writes)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(WishChange(CREATED_ID, WISHLIST_ID)), changes)
    }

    @Test
    fun `a save with every field and an image creates the wish then uploads the rotated photo`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Tent")
        sut.type(WishField.URL, "https://shop.test/tent")
        sut.type(WishField.PRICE, "12.50")
        sut.type(WishField.CURRENCY, "EUR")
        sut.type(WishField.COMMENT, "light")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(
            listOf("createWish:travel:Tent:https://shop.test/tent:12.50:EUR:light", "uploadWish:$CREATED_ID"),
            repository.writes
        )
        assertEquals("$PHOTO@${Rotation.CW_90}", repository.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", repository.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(WishChange(CREATED_ID, WISHLIST_ID)), changes)
    }

    @Test
    fun `the save shows its progress while the server answers`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Tent")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        val create = CompletableDeferred<Result<Wish>>()
        repository.createWishAnswer = { create.await() }

        sut.uiState.value.onSave()
        assertEquals(FormSaveStatus.Sending(done = 0, total = 2), sut.uiState.value.save)
        create.complete(Result.success(wish(id = CREATED_ID, name = "Tent")))

        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a failed create keeps the form reachable and the retry creates again`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Tent")
        repository.createWishAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(changes.isEmpty())

        repository.createWishAnswer = { Result.success(wish(id = CREATED_ID, name = "Tent")) }
        sut.uiState.value.onRetrySave()

        assertEquals(2, repository.writes.count { it.startsWith("createWish:") })
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a 422 names the validation error and the form can be edited again`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Tent")
        sut.type(WishField.CURRENCY, "XXQ")
        val invalid = KoillectionError.Validation("currency: This value is not a valid currency.")
        repository.createWishAnswer = { Result.failure(invalid) }

        sut.uiState.value.onSave()
        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_validation),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        sut.uiState.value.onEditForm()

        assertNull(sut.uiState.value.save)
        assertEquals("XXQ", sut.uiState.value.values.currency)
    }

    @Test
    fun `a failed upload is partial and the retry uploads without a second create`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Tent")
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadWishAnswer = { Result.failure(KoillectionError.Server(500, null)) }

        sut.uiState.value.onSave()
        val failed = sut.uiState.value.save
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_error_quota), isPartial = true),
            failed
        )
        sut.uiState.value.onEditForm()
        assertEquals(failed, sut.uiState.value.save)

        repository.uploadWishAnswer = { Result.success(wish(id = CREATED_ID, name = "Tent")) }
        sut.uiState.value.onRetrySave()

        assertEquals(
            listOf("createWish:travel:Tent:null:null:null:null", "uploadWish:$CREATED_ID", "uploadWish:$CREATED_ID"),
            repository.writes
        )
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `the form survives process death`() {
        val first = createSut()
        first.type(WishField.NAME, "Tent")
        first.type(WishField.URL, "https://shop.test/tent")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))

        val restored = createSut(restoredHandle())

        assertEquals(WishFormValues(name = "Tent", url = "https://shop.test/tent"), restored.uiState.value.values)
        assertEquals(FormPhoto(PHOTO), restored.uiState.value.photo)
        assertNull(restored.uiState.value.save)
    }

    @Test
    fun `after process death in the upload the retry uploads to the created wish`() {
        val first = createSut()
        first.type(WishField.NAME, "Tent")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadWishAnswer = { CompletableDeferred<Result<Wish>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        repository.uploadWishAnswer = { Result.success(wish(id = CREATED_ID, name = "Tent")) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, repository.writes.count { it.startsWith("createWish:") })
        assertEquals(2, repository.writes.count { it == "uploadWish:$CREATED_ID" })
        assertTrue(restored.uiState.value.isSaved)
    }

    private companion object {
        const val WISHLIST_ID = "travel"
        const val PHOTO = "content://photos/1"
    }
}
