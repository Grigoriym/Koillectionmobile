package com.grappim.koillectionmobile.feature.templates.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveTemplateUseCaseTest {

    private val repository = RecordingTemplatesRepository()
    private val sut = SaveTemplateUseCaseImpl(repository)

    /** A template "Book" with Author (text), Read (checkbox), Genre (choice-list "genres"). */
    private fun book(): TemplateSnapshot {
        val template = Template(id = "book", name = "Book")
        repository.templates[template.id] = template
        listOf(
            TemplateField("author", "Author", 1, "text", null),
            TemplateField("read", "Read", 2, "checkbox", null),
            TemplateField("genre", "Genre", 3, "choice-list", "genres")
        ).forEach { repository.fields[it.id] = template.id to it }
        return TemplateSnapshot(template, repository.fieldsOf(template.id))
    }

    private fun TemplateField.kept() = EditedTemplateField(id, name, type, choiceListId)

    private fun newField(name: String, type: String = "text", choiceListId: String? = null) =
        EditedTemplateField(fieldId = null, name = name, type = type, choiceListId = choiceListId)

    @Test
    fun `create sends the template then each field at its position`() = runTest {
        val progress = mutableListOf<SaveTemplateProgress>()
        val edit = TemplateEdit(
            name = "Book",
            fields = listOf(newField("Author"), newField("Genre", "choice-list", "genres"), newField("Pages", "number"))
        )

        val result = sut.save(original = null, edit = edit, onProgress = { progress += it })

        assertEquals(SaveTemplateResult.Success("t1"), result)
        assertEquals(
            listOf(
                "createTemplate Book",
                "createField Author 1 text null",
                "createField Genre 2 choice-list genres",
                "createField Pages 3 number null"
            ),
            repository.calls
        )
        assertEquals(SaveTemplateProgress(1, 4), progress.first())
        assertEquals(SaveTemplateProgress(4, 4), progress.last())
    }

    @Test
    fun `a choice list on a field of another type is not sent`() = runTest {
        sut.save(original = null, edit = TemplateEdit("Book", listOf(newField("Author", "text", "genres"))))

        assertEquals("createField Author 1 text null", repository.calls.last())
    }

    @Test
    fun `a failed template create names no template`() = runTest {
        repository.failOn = "createTemplate"

        val result = sut.save(original = null, edit = TemplateEdit("Book", listOf(newField("Author"))))

        assertEquals(SaveTemplateResult.Failure(repository.error, templateId = null), result)
        assertEquals(listOf("createTemplate Book"), repository.calls)
    }

    @Test
    fun `a create that fails part way is finished by a retry without a second field`() = runTest {
        repository.failOn = "createField Genre"
        val edit = TemplateEdit("Book", listOf(newField("Author"), newField("Genre", "choice-list", "genres")))

        val first = sut.save(original = null, edit = edit)

        assertEquals(SaveTemplateResult.Failure(repository.error, templateId = "t1"), first)
        repository.calls.clear()

        val second = sut.retry(templateId = "t1", original = null, edit = edit)

        assertEquals(SaveTemplateResult.Success("t1"), second)
        assertEquals(listOf("createField Genre 2 choice-list genres"), repository.calls)
        assertEquals(listOf("Author", "Genre"), repository.fieldsOf("t1").map { it.name })
    }

    @Test
    fun `an edit without a change sends nothing`() = runTest {
        val original = book()

        val result = sut.save(original, TemplateEdit("Book", original.fields.map { it.kept() }))

        assertEquals(SaveTemplateResult.Success("book"), result)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `a rename sends only the rename`() = runTest {
        val original = book()

        sut.save(original, TemplateEdit("Novel", original.fields.map { it.kept() }))

        assertEquals(listOf("renameTemplate book Novel"), repository.calls)
    }

    @Test
    fun `a new field goes after the others`() = runTest {
        val original = book()

        sut.save(original, TemplateEdit("Book", original.fields.map { it.kept() } + newField("Pages", "number")))

        assertEquals(listOf("createField Pages 4 number null"), repository.calls)
    }

    @Test
    fun `a removed field is deleted and the fields after it move up`() = runTest {
        val original = book()

        sut.save(original, TemplateEdit("Book", original.fields.drop(1).map { it.kept() }))

        assertEquals(
            listOf("deleteField author", "editField read null 1 null null", "editField genre null 2 null null"),
            repository.calls
        )
    }

    @Test
    fun `a moved field sends the new positions only`() = runTest {
        val original = book()
        val (author, read, genre) = original.fields.map { it.kept() }

        sut.save(original, TemplateEdit("Book", listOf(author, genre, read)))

        assertEquals(listOf("editField genre null 2 null null", "editField read null 3 null null"), repository.calls)
    }

    @Test
    fun `a field edit sends the name type and choice list in one call`() = runTest {
        val original = book()
        val (author, read, _) = original.fields.map { it.kept() }
        val format = EditedTemplateField("genre", "Format", "choice-list", "formats")

        sut.save(original, TemplateEdit("Book", listOf(author, read, format)))

        assertEquals(listOf("editField genre Format null null formats"), repository.calls)
    }

    @Test
    fun `a type change away from choice-list sends no choice list`() = runTest {
        val original = book()
        val (author, read, _) = original.fields.map { it.kept() }
        val genre = EditedTemplateField("genre", "Genre", "text", "genres")

        sut.save(original, TemplateEdit("Book", listOf(author, read, genre)))

        assertEquals(listOf("editField genre null null text null"), repository.calls)
    }

    @Test
    fun `a duplicate field name sends nothing`() = runTest {
        val original = book()

        val result = sut.save(original, TemplateEdit("Book", original.fields.map { it.kept() } + newField("Author")))

        assertEquals(SaveTemplateResult.DuplicateName("Author"), result)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `an edit that fails part way is finished by a retry that sends only the rest`() = runTest {
        val original = book()
        val (author, read, genre) = original.fields.map { it.kept() }
        val edit = TemplateEdit("Novel", listOf(genre, author.copy(name = "Writer"), newField("Pages")))
        repository.failOn = "editField author"

        val first = sut.save(original, edit)

        assertEquals(SaveTemplateResult.Failure(repository.error, templateId = "book"), first)
        assertEquals(
            listOf(
                "deleteField read",
                "renameTemplate book Novel",
                "editField genre null 1 null null",
                "editField author Writer 2 null null"
            ),
            repository.calls
        )
        repository.calls.clear()

        val second = sut.retry(templateId = "book", original = original, edit = edit)

        assertEquals(SaveTemplateResult.Success("book"), second)
        assertEquals(listOf("editField author Writer 2 null null", "createField Pages 3 text null"), repository.calls)
        assertEquals(listOf("Genre", "Writer", "Pages"), repository.fieldsOf("book").map { it.name })
    }
}
