package com.grappim.koillectionmobile.feature.items.domain

import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class AddItemUseCaseTest {

    private val server = FakeServer()
    private val useCase = AddItemUseCaseImpl(server.items, server.tags)

    @Test
    fun `addItem sends every part in order with positions`() = runTest {
        val result = useCase.addItem(BOOK)

        assertEquals(AddItemResult.Success(ITEM_ID), result)
        assertEquals(ALL_CALLS, server.calls)
    }

    @Test
    fun `addItem without main image and tags sends neither`() = runTest {
        val result = useCase.addItem(BOOK.copy(mainImage = null, tagIds = emptyList()))

        assertEquals(AddItemResult.Success(ITEM_ID), result)
        assertEquals(ALL_CALLS - "uploadItemImage item-1 main.jpg" - "setItemTags item-1 [tag-a, tag-b]", server.calls)
    }

    @Test
    fun `a failure at any call stops there and a retry sends only the rest`() = runTest {
        for (failing in ALL_CALLS) {
            server.calls.clear()
            server.failOnce = failing

            val failure = assertIs<AddItemResult.Failure>(useCase.addItem(BOOK), failing)
            assertSame(server.error, failure.error, failing)
            val failedAt = ALL_CALLS.indexOf(failing)
            assertEquals(ALL_CALLS.take(failedAt + 1), server.calls, failing)

            server.calls.clear()
            val retry = useCase.addItem(BOOK, failure.state)

            assertEquals(AddItemResult.Success(ITEM_ID), retry, failing)
            assertEquals(ALL_CALLS.drop(failedAt), server.calls, failing)
        }
    }

    @Test
    fun `a failed image upload keeps the created field and the retry uploads to it`() = runTest {
        server.failOnce = "uploadDatumImage datum-Spine spine.jpg"

        val failure = assertIs<AddItemResult.Failure>(useCase.addItem(BOOK))

        assertEquals(
            AddItemState(
                itemId = ITEM_ID,
                textFieldsDone = 2,
                mainImageDone = true,
                imageFieldIds = listOf("datum-Title page", "datum-Spine"),
                imageFieldsUploaded = 1
            ),
            failure.state
        )
        server.calls.clear()
        useCase.addItem(BOOK, failure.state)
        assertEquals(
            listOf("uploadDatumImage datum-Spine spine.jpg", "setItemTags item-1 [tag-a, tag-b]"),
            server.calls
        )
    }

    @Test
    fun `progress counts every call up to the total`() = runTest {
        val progress = mutableListOf<AddItemProgress>()

        useCase.addItem(BOOK) { progress += it }

        val total = ALL_CALLS.size
        assertEquals((0..total).map { it to total }, progress.map { it.done to it.total })
    }

    @Test
    fun `progress of a retry starts at what is already done`() = runTest {
        server.failOnce = "createImageDatum item-1 Spine 4"
        val failure = assertIs<AddItemResult.Failure>(useCase.addItem(BOOK))
        val progress = mutableListOf<AddItemProgress>()

        useCase.addItem(BOOK, failure.state) { progress += it }

        val total = ALL_CALLS.size
        assertEquals(6 to total, progress.first().let { it.done to it.total })
        assertEquals(total to total, progress.last().let { it.done to it.total })
    }

    @Test
    fun `the state of the last progress before a stop resumes the save`() = runTest {
        server.failOnce = "createTextDatum item-1 Year 1901 2"
        val progress = mutableListOf<AddItemProgress>()
        val failure = assertIs<AddItemResult.Failure>(useCase.addItem(BOOK) { progress += it })
        assertEquals(failure.state, progress.last().state)

        server.calls.clear()
        useCase.addItem(BOOK, progress.last().state)

        assertEquals(ALL_CALLS.drop(ALL_CALLS.indexOf("createTextDatum item-1 Year 1901 2")), server.calls)
    }

    /** Both repositories, recording each call as one line. [failOnce] fails the call with that line once. */
    private class FakeServer {
        val calls = mutableListOf<String>()
        var failOnce: String? = null
        val error = IllegalStateException("server down")

        private fun <T> answer(call: String, value: () -> T): Result<T> {
            calls += call
            if (call == failOnce) {
                failOnce = null
                return Result.failure(error)
            }
            return Result.success(value())
        }

        val items = object : ItemsRepository {
            override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

            override suspend fun getItem(id: String): Result<Item> = error("not used")

            override suspend fun getData(itemId: String): Result<List<Datum>> = error("not used")

            override suspend fun createItem(collectionId: String, name: String): Result<Item> =
                answer("createItem $collectionId $name") { Item(ITEM_ID, name, 1, collectionId, null) }

            override suspend fun createTextDatum(itemId: String, label: String, value: String, position: Int) =
                answer("createTextDatum $itemId $label $value $position") {
                    Datum("datum-$label", "text", label, value, position, null)
                }

            override suspend fun createImageDatum(itemId: String, label: String, position: Int) =
                answer("createImageDatum $itemId $label $position") {
                    Datum("datum-$label", "image", label, null, position, null)
                }

            override suspend fun uploadItemImage(itemId: String, bytes: ByteArray, fileName: String, mimeType: String) =
                answer("uploadItemImage $itemId $fileName") { Item(itemId, "", 1, null, null) }

            override suspend fun uploadDatumImage(
                datumId: String,
                bytes: ByteArray,
                fileName: String,
                mimeType: String
            ) = answer("uploadDatumImage $datumId $fileName") { Datum(datumId, "image", null, null, null, null) }
        }

        val tags = object : TagsRepository {
            override suspend fun getTags(): Result<List<Tag>> = error("not used")

            override suspend fun getTagCategories(): Result<List<TagCategory>> = error("not used")

            override suspend fun getItemTags(itemId: String): Result<List<Tag>> = error("not used")

            override suspend fun setItemTags(itemId: String, tagIds: List<String>) =
                answer("setItemTags $itemId $tagIds") { }
        }
    }

    private companion object {
        const val ITEM_ID = "item-1"

        val BOOK = NewItem(
            collectionId = "books",
            name = "Jörn Uhl",
            textFields = listOf(NewTextField("Author", "Frenssen"), NewTextField("Year", "1901")),
            mainImage = NewImage(byteArrayOf(1), "main.jpg", "image/jpeg"),
            imageFields = listOf(
                NewImageField("Title page", NewImage(byteArrayOf(2), "title.jpg", "image/jpeg")),
                NewImageField("Spine", NewImage(byteArrayOf(3), "spine.jpg", "image/jpeg"))
            ),
            tagIds = listOf("tag-a", "tag-b")
        )

        val ALL_CALLS = listOf(
            "createItem books Jörn Uhl",
            "createTextDatum item-1 Author Frenssen 1",
            "createTextDatum item-1 Year 1901 2",
            "uploadItemImage item-1 main.jpg",
            "createImageDatum item-1 Title page 3",
            "uploadDatumImage datum-Title page title.jpg",
            "createImageDatum item-1 Spine 4",
            "uploadDatumImage datum-Spine spine.jpg",
            "setItemTags item-1 [tag-a, tag-b]"
        )
    }
}
