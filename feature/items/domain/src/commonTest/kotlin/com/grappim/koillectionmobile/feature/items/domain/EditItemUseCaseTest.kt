package com.grappim.koillectionmobile.feature.items.domain

import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class EditItemUseCaseTest {

    private val server = FakeServer()
    private val useCase = EditItemUseCaseImpl(server.items, server.tags)

    private suspend fun edit(edit: ItemEdit): EditItemResult = useCase.editItem(server.snapshot(), edit)

    @Test
    fun `no change sends no call`() = runTest {
        assertEquals(EditItemResult.Success, edit(UNCHANGED))
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun `a rename alone`() = runTest {
        edit(UNCHANGED.copy(name = "Hilligenlei"))
        assertEquals(listOf("renameItem Hilligenlei"), server.calls)
    }

    @Test
    fun `a text value change alone sends only the value`() = runTest {
        edit(UNCHANGED.withField(EditedField.Text("author", "Author", "G. Frenssen")))
        assertEquals(listOf("updateDatum author null G. Frenssen"), server.calls)
    }

    @Test
    fun `a label change alone sends only the label`() = runTest {
        edit(UNCHANGED.withField(EditedField.Text("author", "Writer", "Frenssen")))
        assertEquals(listOf("updateDatum author Writer null"), server.calls)
    }

    @Test
    fun `an image field label change does not upload`() = runTest {
        edit(UNCHANGED.withField(EditedField.Image("cover", "Front", null)))
        assertEquals(listOf("updateDatum cover Front null"), server.calls)
    }

    @Test
    fun `a field left out of the form is deleted`() = runTest {
        edit(UNCHANGED.copy(fields = UNCHANGED.fields.filter { it.datumId != "cover" }))
        assertEquals(listOf("deleteDatum cover"), server.calls)
    }

    @Test
    fun `a field image replacement alone`() = runTest {
        edit(UNCHANGED.withField(EditedField.Image("cover", "Cover", image("new-cover.jpg"))))
        assertEquals(listOf("uploadDatumImage cover new-cover.jpg"), server.calls)
    }

    @Test
    fun `a new text field goes after the last field`() = runTest {
        edit(UNCHANGED.copy(newTextFields = listOf(NewTextField("Pages", "412"))))
        assertEquals(listOf("createTextDatum Pages 412 5"), server.calls)
    }

    @Test
    fun `a new image field is created then its image uploaded`() = runTest {
        edit(UNCHANGED.copy(newImageFields = listOf(NewImageField("Spine", image("spine.jpg")))))
        assertEquals(listOf("createImageDatum Spine 5", "uploadDatumImage datum-1 spine.jpg"), server.calls)
    }

    @Test
    fun `a new main image alone`() = runTest {
        edit(UNCHANGED.copy(newMainImage = image("main.jpg")))
        assertEquals(listOf("uploadItemImage main.jpg"), server.calls)
    }

    @Test
    fun `changed tags send the whole list`() = runTest {
        edit(UNCHANGED.copy(tagIds = listOf("tag-a", "tag-c")))
        assertEquals(listOf("setItemTags [tag-a, tag-c]"), server.calls)
    }

    @Test
    fun `the same tags in another order send no call`() = runTest {
        edit(UNCHANGED.copy(tagIds = listOf("tag-b", "tag-a")))
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun `a new tag is created then set with the picked tags`() = runTest {
        assertEquals(EditItemResult.Success, edit(UNCHANGED.copy(newTagLabels = listOf("Signed"))))

        assertEquals(listOf("getTags", "createTag Signed", "setItemTags [tag-a, tag-b, tag-1]"), server.calls)
    }

    @Test
    fun `a new label that a server tag has in another case uses that tag`() = runTest {
        assertEquals(EditItemResult.Success, edit(UNCHANGED.copy(newTagLabels = listOf("classic"))))

        assertEquals(listOf("getTags", "setItemTags [tag-a, tag-b, tag-c]"), server.calls)
    }

    @Test
    fun `a failed tag create is retried without a second tag`() = runTest {
        for (applied in listOf(false, true)) {
            server.reset()
            val original = server.snapshot()
            val edit = UNCHANGED.copy(newTagLabels = listOf("Signed"))
            server.fail("createTag Signed", applied)

            assertIs<EditItemResult.Failure>(useCase.editItem(original, edit), "applied: $applied")
            server.calls.clear()
            assertEquals(EditItemResult.Success, useCase.retry(original, edit), "applied: $applied")

            val retryCalls =
                listOfNotNull("getTags", "createTag Signed".takeUnless { applied }, "setItemTags [tag-a, tag-b, tag-1]")
            assertEquals(retryCalls, server.calls, "applied: $applied")
            assertEquals(1, server.serverTags.count { it.label == "Signed" }, "applied: $applied")
            assertEquals(setOf("tag-a", "tag-b", "tag-1"), server.tagIds, "applied: $applied")
        }
    }

    @Test
    fun `a mix sends deletes then changes then new fields then main image then tags`() = runTest {
        assertEquals(EditItemResult.Success, edit(MIX))
        assertEquals(MIX_CALLS, server.calls)
        assertEquals(MIX_RESULT, server.contents())
    }

    @Test
    fun `a freed label can be used by a new field`() = runTest {
        val result = edit(
            UNCHANGED.copy(
                fields = UNCHANGED.fields.filter { it.datumId != "cover" },
                newImageFields = listOf(NewImageField("Cover", image("cover2.jpg")))
            )
        )

        assertEquals(EditItemResult.Success, result)
        assertEquals(
            listOf("deleteDatum cover", "createImageDatum Cover 5", "uploadDatumImage datum-1 cover2.jpg"),
            server.calls
        )
    }

    @Test
    fun `a failure stops there and the retry sends only what did not go through`() = runTest {
        for (failing in MIX_CALLS) {
            for (applied in listOf(false, true)) {
                val case = "$failing, applied: $applied"
                server.reset()
                val original = server.snapshot()
                server.fail(failing, applied)

                val failure = assertIs<EditItemResult.Failure>(useCase.editItem(original, MIX), case)
                assertSame(server.error, failure.error, case)
                assertEquals(failing.startsWith("upload"), failure.isUpload, case)
                val failedAt = MIX_CALLS.indexOf(failing)
                assertEquals(MIX_CALLS.take(failedAt + 1), server.calls, case)

                server.calls.clear()
                assertEquals(EditItemResult.Success, useCase.retry(original, MIX), case)
                assertEquals(MIX_CALLS.drop(if (applied) failedAt + 1 else failedAt), server.calls, case)
                assertEquals(MIX_RESULT, server.contents(), case)
            }
        }
    }

    @Test
    fun `a failed load on retry sends no write`() = runTest {
        val original = server.snapshot()
        server.loadFails = true

        assertIs<EditItemResult.Failure>(useCase.retry(original, MIX))
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun `a label swap goes through a temporary label`() = runTest {
        val result = edit(
            UNCHANGED.withField(EditedField.Text("author", "Year", "Frenssen"))
                .withField(EditedField.Text("year", "Author", "1901"))
        )

        assertEquals(EditItemResult.Success, result)
        assertEquals(
            listOf(
                "updateDatum author Author (moving 1) null",
                "updateDatum year Author null",
                "updateDatum author Year null"
            ),
            server.calls
        )
        assertEquals(listOf("Year", "Author"), server.data.take(2).map { it.label })
    }

    @Test
    fun `a label chain is ordered without a temporary label`() = runTest {
        val result = edit(
            UNCHANGED.withField(EditedField.Text("author", "Year", "Frenssen"))
                .withField(EditedField.Text("year", "Published", "1901"))
        )

        assertEquals(EditItemResult.Success, result)
        assertEquals(listOf("updateDatum year Published null", "updateDatum author Year null"), server.calls)
    }

    @Test
    fun `a duplicate label sends no call`() = runTest {
        val result = edit(
            UNCHANGED.withField(EditedField.Text("author", "Year", "Frenssen"))
                .copy(newTextFields = listOf(NewTextField("Pages", "412")))
        )

        assertEquals(EditItemResult.DuplicateLabel("Year"), result)
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun `a new field with the label of an untouched field is a duplicate`() = runTest {
        val result = edit(UNCHANGED.copy(newTextFields = listOf(NewTextField("Read on", "today"))))

        assertEquals(EditItemResult.DuplicateLabel("Read on"), result)
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun `a field of another type is never touched`() = runTest {
        // The form has no row for the date field, and a text edit names it by mistake.
        val result = edit(UNCHANGED.copy(fields = UNCHANGED.fields + EditedField.Text("date", "Read", "2020")))

        assertEquals(EditItemResult.Success, result)
        assertEquals(emptyList(), server.calls)
    }

    @Test
    fun `progress counts every call up to the total`() = runTest {
        val progress = mutableListOf<EditItemProgress>()

        useCase.editItem(server.snapshot(), MIX) { progress += it }

        val total = MIX_CALLS.size
        assertEquals((0..total).map { EditItemProgress(it, total) }, progress)
    }

    /**
     * One item on a server that keeps state, so a retry reads what earlier calls did. Records each
     * write as one line. Rejects a label already used on the item, as the server does.
     */
    private class FakeServer {
        val calls = mutableListOf<String>()
        val error = IllegalStateException("server down")
        var loadFails = false
        private var failing: String? = null
        private var failAfterApply = false

        lateinit var item: Item
        lateinit var data: MutableList<Datum>
        lateinit var tagIds: Set<String>
        lateinit var serverTags: MutableList<Tag>
        private var nextId = 0
        private var nextTagId = 0

        init {
            reset()
        }

        fun reset() {
            calls.clear()
            item = Item(ITEM_ID, "Jörn Uhl", 1, "books", imageOf("main-0"))
            data = START_DATA.toMutableList()
            tagIds = setOf("tag-a", "tag-b")
            serverTags = SERVER_TAGS.toMutableList()
            nextId = 0
            nextTagId = 0
        }

        /** Fails [call] once: before it changes anything, or, with [applied], after (a lost answer). */
        fun fail(call: String, applied: Boolean) {
            failing = call
            failAfterApply = applied
        }

        fun snapshot() = ItemSnapshot(item, data.toList(), tagIds)

        /** What a reader sees, with server ids of new fields left out (they depend on the call count). */
        fun contents() = Triple(
            item.name to item.image?.url?.substringBefore('#'),
            data.map { listOf(it.type, it.label, it.value, it.position, it.image?.url?.substringBefore('#')) },
            tagIds
        )

        private fun <T> write(call: String, apply: () -> T): Result<T> {
            calls += call
            if (call == failing && !failAfterApply) {
                failing = null
                return Result.failure(error)
            }
            val value = runCatching(apply)
            if (call == failing) {
                failing = null
                return Result.failure(error)
            }
            return value
        }

        private fun checkFree(label: String, except: String?) =
            check(data.none { it.id != except && it.label == label }) { "label $label is used" }

        private fun replace(id: String, change: (Datum) -> Datum): Datum {
            val index = data.indexOfFirst { it.id == id }
            check(index >= 0) { "no field $id" }
            return change(data[index]).also { data[index] = it }
        }

        private fun newFile(name: String): Image = imageOf("$name#${nextId++}")

        val items = object : ItemsRepository {
            override suspend fun getTagItems(tagId: String): Result<List<Item>> = error("not used")

            override suspend fun getItems(collectionId: String): Result<List<Item>> = error("not used")

            override suspend fun getAllItems(): Result<List<Item>> = error("not used")

            override suspend fun getItem(id: String): Result<Item> =
                if (loadFails) Result.failure(error) else Result.success(item)

            override suspend fun getData(itemId: String): Result<List<Datum>> = Result.success(data.toList())

            override suspend fun createItem(collectionId: String, name: String): Result<Item> = error("not used")

            override suspend fun createTextDatum(itemId: String, label: String, value: String, position: Int) =
                write("createTextDatum $label $value $position") {
                    checkFree(label, null)
                    Datum("datum-${++nextId}", "text", label, value, position, null).also { data += it }
                }

            override suspend fun createImageDatum(itemId: String, label: String, position: Int) =
                write("createImageDatum $label $position") {
                    checkFree(label, null)
                    Datum("datum-${++nextId}", "image", label, null, position, null).also { data += it }
                }

            override suspend fun uploadItemImage(itemId: String, bytes: ByteArray, fileName: String, mimeType: String) =
                write("uploadItemImage $fileName") { item.copy(image = newFile(fileName)).also { item = it } }

            override suspend fun uploadDatumImage(
                datumId: String,
                bytes: ByteArray,
                fileName: String,
                mimeType: String
            ) = write("uploadDatumImage $datumId $fileName") { replace(datumId) { it.copy(image = newFile(fileName)) } }

            override suspend fun renameItem(id: String, name: String): Result<Item> =
                write("renameItem $name") { item.copy(name = name).also { item = it } }

            override suspend fun updateDatum(id: String, label: String?, value: String?): Result<Datum> =
                write("updateDatum $id $label $value") {
                    label?.let { checkFree(it, id) }
                    replace(id) { it.copy(label = label ?: it.label, value = value ?: it.value) }
                }

            override suspend fun deleteDatum(id: String): Result<Unit> =
                write("deleteDatum $id") { check(data.removeAll { it.id == id }) { "no field $id" } }

            override suspend fun deleteItem(id: String): Result<Unit> = error("not used")
        }

        val tags = object : TagsRepository {
            override suspend fun getTags(): Result<List<Tag>> {
                calls += "getTags"
                return Result.success(serverTags.toList())
            }

            override suspend fun getTagCategories(): Result<List<TagCategory>> = error("not used")

            override suspend fun getItemTags(itemId: String): Result<List<Tag>> =
                Result.success(tagIds.map { Tag(it, it, null, null) })

            override suspend fun createTag(label: String): Result<Tag> = write("createTag $label") {
                Tag("tag-${++nextTagId}", label, null, null).also { serverTags += it }
            }

            override suspend fun setItemTags(itemId: String, tagIds: List<String>) =
                write("setItemTags $tagIds") { this@FakeServer.tagIds = tagIds.toSet() }
        }
    }

    private companion object {
        const val ITEM_ID = "item-1"

        fun imageOf(url: String) = Image(url, null, null)

        val SERVER_TAGS = listOf(
            Tag("tag-a", "Novel", null, null),
            Tag("tag-b", "German", null, null),
            Tag("tag-c", "Classic", null, null)
        )

        fun image(fileName: String) = NewImage(byteArrayOf(1), fileName, "image/jpeg")

        val START_DATA = listOf(
            Datum("author", "text", "Author", "Frenssen", 1, null),
            Datum("year", "text", "Year", "1901", 2, null),
            Datum("cover", "image", "Cover", null, 3, imageOf("cover-0")),
            Datum("date", "date", "Read on", "2020-01-01", 4, null)
        )

        /** The form as loaded: every text and image field, nothing changed. */
        val UNCHANGED = ItemEdit(
            name = "Jörn Uhl",
            fields = listOf(
                EditedField.Text("author", "Author", "Frenssen"),
                EditedField.Text("year", "Year", "1901"),
                EditedField.Image("cover", "Cover", null)
            ),
            newTextFields = emptyList(),
            newImageFields = emptyList(),
            newMainImage = null,
            tagIds = listOf("tag-a", "tag-b")
        )

        fun ItemEdit.withField(field: EditedField) =
            copy(fields = fields.map { if (it.datumId == field.datumId) field else it })

        /** Every kind of change, and a label freed by a delete taken by a change. */
        val MIX = UNCHANGED.copy(
            name = "Hilligenlei",
            fields = listOf(
                EditedField.Text("author", "Cover", "G. Frenssen"),
                EditedField.Text("year", "Year", "1905")
            ),
            newTextFields = listOf(NewTextField("Pages", "412")),
            newImageFields = listOf(NewImageField("Spine", image("spine.jpg"))),
            newMainImage = image("main.jpg"),
            tagIds = listOf("tag-c")
        )

        val MIX_CALLS = listOf(
            "deleteDatum cover",
            "renameItem Hilligenlei",
            "updateDatum author Cover G. Frenssen",
            "updateDatum year null 1905",
            "createTextDatum Pages 412 5",
            "createImageDatum Spine 6",
            "uploadDatumImage datum-2 spine.jpg",
            "uploadItemImage main.jpg",
            "setItemTags [tag-c]"
        )

        val MIX_RESULT = Triple(
            "Hilligenlei" to "main.jpg",
            listOf(
                listOf("text", "Cover", "G. Frenssen", 1, null),
                listOf("text", "Year", "1905", 2, null),
                listOf("date", "Read on", "2020-01-01", 4, null),
                listOf("text", "Pages", "412", 5, null),
                listOf("image", "Spine", null, 6, "spine.jpg")
            ),
            setOf("tag-c")
        )
    }
}
