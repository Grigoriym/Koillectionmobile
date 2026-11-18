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
    fun `a new tag is created then set with the picked tags`() = runTest {
        val result = useCase.addItem(BOOK.copy(newTagLabels = listOf("Signed")))

        assertEquals(AddItemResult.Success(ITEM_ID), result)
        assertEquals(
            ALL_CALLS.dropLast(1) + listOf("getTags", "createTag Signed", "setItemTags item-1 [tag-a, tag-b, tag-new]"),
            server.calls
        )
    }

    @Test
    fun `a new label that a server tag has in another case uses that tag`() = runTest {
        useCase.addItem(BOOK.copy(tagIds = emptyList(), newTagLabels = listOf("classic")))

        assertEquals(ALL_CALLS.dropLast(1) + listOf("getTags", "setItemTags item-1 [tag-c]"), server.calls)
    }

    @Test
    fun `a failed tag create is retried without a second tag`() = runTest {
        for (applied in listOf(false, true)) {
            server.calls.clear()
            server.serverTags.removeAll { it.label == "Signed" }
            server.failOnce = "createTag Signed"
            server.failAfterApply = applied
            val item = BOOK.copy(newTagLabels = listOf("Signed"))

            val failure = assertIs<AddItemResult.Failure>(useCase.addItem(item), "applied: $applied")
            server.calls.clear()
            assertEquals(AddItemResult.Success(ITEM_ID), useCase.addItem(item, failure.state), "applied: $applied")

            val retryCalls = listOfNotNull(
                "getTags",
                "createTag Signed".takeUnless { applied },
                "setItemTags item-1 [tag-a, tag-b, tag-new]"
            )
            assertEquals(retryCalls, server.calls, "applied: $applied")
            assertEquals(1, server.serverTags.count { it.label == "Signed" }, "applied: $applied")
        }
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

    @Test
    fun `a typed field is created with its type and its choice list`() = runTest {
        val book = BOOK.copy(
            textFields = listOf(
                NewTextField("Signed", "1", TYPE_CHECKBOX),
                NewTextField("Format", """["Hardcover","Ebook"]""", TYPE_CHOICE_LIST, "list-1")
            ),
            mainImage = null,
            imageFields = emptyList(),
            tagIds = emptyList()
        )

        useCase.addItem(book)

        assertEquals(
            listOf(
                "createItem books Jörn Uhl",
                "createTextDatum item-1 Signed 1 1 checkbox null",
                """createTextDatum item-1 Format ["Hardcover","Ebook"] 2 choice-list list-1"""
            ),
            server.calls
        )
    }

    /** Both repositories, recording each call as one line. [failOnce] fails the call with that line once. */
    private class FakeServer {
        val calls = mutableListOf<String>()
        var failOnce: String? = null

        /** The failing call of [failOnce] goes through first: a lost answer. */
        var failAfterApply = false
        val error = IllegalStateException("server down")
        val serverTags = mutableListOf(Tag("tag-a", "Novel", null, null), Tag("tag-c", "Classic", null, null))

        private fun <T> answer(call: String, value: () -> T): Result<T> {
            calls += call
            if (call == failOnce) {
                failOnce = null
                if (failAfterApply) value()
                return Result.failure(error)
            }
            return Result.success(value())
        }

        val items = object : ItemsRepository {
            override suspend fun getTagItems(tagId: String): Result<List<Item>> = error("not used")

            override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

            override suspend fun getAllItems(): Result<List<Item>> = error("not used")

            override suspend fun getItem(id: String): Result<Item> = error("not used")

            override suspend fun getData(itemId: String): Result<List<Datum>> = error("not used")

            override suspend fun createItem(collectionId: String, name: String): Result<Item> =
                answer("createItem $collectionId $name") { Item(ITEM_ID, name, 1, collectionId, null) }

            override suspend fun createTextDatum(
                itemId: String,
                label: String,
                value: String,
                position: Int,
                type: String,
                choiceListId: String?
            ) = answer("createTextDatum $itemId $label $value $position" + typeSuffix(type, choiceListId)) {
                Datum("datum-$label", type, label, value, position, null, choiceListId)
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

            override suspend fun renameItem(id: String, name: String): Result<Item> = error("not used")

            override suspend fun updateDatum(id: String, label: String?, value: String?): Result<Datum> =
                error("not used")

            override suspend fun deleteDatum(id: String): Result<Unit> = error("not used")

            override suspend fun deleteItem(id: String): Result<Unit> = error("not used")
        }

        val tags = object : TagsRepository {
            override suspend fun getTags(): Result<List<Tag>> = answer("getTags") { serverTags.toList() }

            override suspend fun getTagCategories(): Result<List<TagCategory>> = error("not used")

            override suspend fun getItemTags(itemId: String): Result<List<Tag>> = error("not used")

            override suspend fun createTag(label: String): Result<Tag> =
                answer("createTag $label") { Tag("tag-new", label, null, null).also { serverTags += it } }

            override suspend fun setItemTags(itemId: String, tagIds: List<String>) =
                answer("setItemTags $itemId $tagIds") { }
        }
    }

    private companion object {
        const val ITEM_ID = "item-1"

        /** A `text` field's call line is as before typed fields; another type adds its type and choice list. */
        fun typeSuffix(type: String, choiceListId: String?) = if (type == TYPE_TEXT) "" else " $type $choiceListId"

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
