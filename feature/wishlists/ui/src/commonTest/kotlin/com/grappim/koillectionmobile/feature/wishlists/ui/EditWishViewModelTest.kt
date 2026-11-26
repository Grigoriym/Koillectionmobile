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
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_unfinished
import com.grappim.koillectionmobile.strings.generated.resources.error_network
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

class EditWishViewModelTest {

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

    /** The wish read answers [loaded] at once. */
    private fun createSut(
        handle: SavedStateHandle = savedStateHandle,
        loaded: Result<Wish> = Result.success(TENT)
    ): EditWishViewModel {
        repository.wish.clear()
        repository.wish.addLast(CompletableDeferred(loaded))
        return EditWishViewModel(TENT.id, repository, photoFiles, wishChanges, handle)
    }

    private fun restoredHandle() =
        SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any>(it) })

    private fun EditWishViewModel.type(field: WishField, text: String) = uiState.value.onFieldChange(field, text)

    @Test
    fun `the form is filled from the server and an unchanged form sends no call`() {
        val sut = createSut()

        val state = sut.uiState.value
        assertEquals(
            WishFormValues(name = "Tent", url = "https://shop.test/tent", price = "12.50", currency = "EUR"),
            state.values
        )
        assertEquals(IMAGE_URL, state.original?.imageUrl)
        assertFalse(state.canSave)
        state.onSave()

        assertNull(sut.uiState.value.save)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `each changed field alone sends one patch with only that key then tells the screens`() {
        val edits = mapOf(
            WishField.NAME to "Big tent",
            WishField.URL to "https://shop.test/big-tent",
            WishField.PRICE to "15",
            WishField.CURRENCY to "USD",
            WishField.COMMENT to "light"
        )
        edits.forEach { (field, text) ->
            repository.writes.clear()
            repository.wishEdits.clear()
            changes.clear()
            val sut = createSut(handle = SavedStateHandle())
            sut.type(field, text)

            sut.uiState.value.onSave()

            assertEquals(listOf("editWish:tent"), repository.writes, field.name)
            assertEquals(listOf(mapOf<WishField, String?>(field to text)), repository.wishEdits, field.name)
            assertTrue(sut.uiState.value.isSaved, field.name)
            assertEquals(listOf(WishChange(TENT.id, WISHLIST_ID)), changes, field.name)
        }
    }

    @Test
    fun `a cleared field is sent as null`() {
        val sut = createSut()
        sut.type(WishField.URL, "")

        sut.uiState.value.onSave()

        assertEquals(listOf(mapOf<WishField, String?>(WishField.URL to null)), repository.wishEdits)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a value typed back to the loaded one is no change`() {
        val sut = createSut()
        sut.type(WishField.PRICE, "20")
        sut.type(WishField.PRICE, " 12.50 ")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `an invalid currency cannot be saved`() {
        val sut = createSut()
        sut.type(WishField.CURRENCY, "EURO")

        sut.uiState.value.onSave()

        assertFalse(sut.uiState.value.canSave)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a replaced image alone uploads the rotated photo and sends no patch`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))
        sut.uiState.value.onPhotoRotate()

        sut.uiState.value.onSave()

        assertEquals(listOf("uploadWish:tent"), repository.writes)
        assertEquals("$PHOTO@${Rotation.CW_90}", repository.uploads.single().bytes.decodeToString())
        assertEquals("image/jpeg", repository.uploads.single().mimeType)
        assertEquals(listOf(PHOTO), photoFiles.deleted)
        assertTrue(sut.uiState.value.isSaved)
    }

    @Test
    fun `a removed new photo leaves the server image and no change`() {
        val sut = createSut()
        sut.uiState.value.onPhotosAdd(listOf(PHOTO))

        sut.uiState.value.onPhotoRemove()

        assertNull(sut.uiState.value.photo)
        assertEquals(IMAGE_URL, sut.uiState.value.original?.imageUrl)
        assertTrue(sut.uiState.value.changes.isEmpty())
    }

    @Test
    fun `a failed patch keeps the form reachable and tells no screen`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Big tent")
        repository.editWishAnswer = { Result.failure(KoillectionError.Network(IllegalStateException("offline"))) }

        sut.uiState.value.onSave()

        assertEquals(
            FormSaveStatus.Failed(
                NativeText.Resource(RString.collection_form_save_error_network),
                isPartial = false
            ),
            sut.uiState.value.save
        )
        assertTrue(changes.isEmpty())
        sut.uiState.value.onEditForm()
        assertNull(sut.uiState.value.save)
        assertEquals("Big tent", sut.uiState.value.values.name)
    }

    @Test
    fun `a failed upload after the patch is partial and the retry sends only the upload`() {
        val sut = createSut()
        sut.type(WishField.NAME, "Big tent")
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

        repository.uploadWishAnswer = { Result.success(TENT) }
        sut.uiState.value.onRetrySave()

        assertEquals(listOf("editWish:tent", "uploadWish:tent", "uploadWish:tent"), repository.writes)
        assertEquals(1, photoFiles.prepared.size)
        assertTrue(sut.uiState.value.isSaved)
        assertEquals(listOf(WishChange(TENT.id, WISHLIST_ID)), changes)
    }

    @Test
    fun `a failed load shows the error and the retry loads again`() {
        val sut = createSut(loaded = Result.failure(KoillectionError.Network(IllegalStateException("offline"))))
        assertEquals(NativeText.Resource(RString.error_network), sut.uiState.value.error)
        assertNull(sut.uiState.value.original)
        assertFalse(sut.uiState.value.canSave)

        repository.wish.clear()
        repository.wish.addLast(CompletableDeferred(Result.success(TENT)))
        sut.uiState.value.onRetry()

        assertEquals("Tent", sut.uiState.value.values.name)
        assertTrue(sut.uiState.value.error.isEmpty())
        assertEquals(2, repository.wishCalls.size)
    }

    @Test
    fun `the edits survive process death without a second load`() {
        val first = createSut()
        first.type(WishField.COMMENT, "light")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))

        val restored = createSut(restoredHandle())

        assertEquals("light", restored.uiState.value.values.comment)
        assertEquals(FormPhoto(PHOTO), restored.uiState.value.photo)
        assertEquals(IMAGE_URL, restored.uiState.value.original?.imageUrl)
        assertNull(restored.uiState.value.save)
        assertEquals(1, repository.wishCalls.size)
    }

    @Test
    fun `after process death in a save the retry sends only the calls not done`() {
        val first = createSut()
        first.type(WishField.NAME, "Big tent")
        first.uiState.value.onPhotosAdd(listOf(PHOTO))
        repository.uploadWishAnswer = { CompletableDeferred<Result<Wish>>().await() }
        first.uiState.value.onSave()

        val restored = createSut(restoredHandle())
        assertEquals(
            FormSaveStatus.Failed(NativeText.Resource(RString.collection_form_save_unfinished), isPartial = true),
            restored.uiState.value.save
        )
        repository.uploadWishAnswer = { Result.success(TENT) }
        restored.uiState.value.onRetrySave()

        assertEquals(1, repository.writes.count { it.startsWith("editWish:") })
        assertEquals(2, repository.writes.count { it.startsWith("uploadWish:") })
        assertTrue(restored.uiState.value.isSaved)
    }

    private companion object {
        const val WISHLIST_ID = "travel"
        const val PHOTO = "content://photos/1"
        const val IMAGE_URL = "https://koillection.test/uploads/tent.jpg"
        val TENT = Wish(
            id = "tent",
            name = "Tent",
            wishlistId = WISHLIST_ID,
            url = "https://shop.test/tent",
            price = "12.50",
            currency = "EUR",
            comment = null,
            imageUrl = IMAGE_URL,
            thumbnailUrl = IMAGE_URL
        )
    }
}
