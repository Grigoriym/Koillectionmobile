package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.core.photos.PhotoFiles
import com.grappim.koillectionmobile.feature.collections.domain.Collection
import com.grappim.koillectionmobile.feature.collections.domain.CollectionsRepository
import com.grappim.koillectionmobile.feature.items.domain.AddItemProgress
import com.grappim.koillectionmobile.feature.items.domain.AddItemResult
import com.grappim.koillectionmobile.feature.items.domain.AddItemState
import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.NewItem
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.templates.domain.ChoiceList
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.feature.templates.domain.TemplateField
import com.grappim.koillectionmobile.feature.templates.domain.TemplatesRepository
import kotlinx.coroutines.CompletableDeferred

internal class FakeTagsRepository : TagsRepository {
    val tags = ArrayDeque<CompletableDeferred<Result<List<Tag>>>>()
    val categories = ArrayDeque<CompletableDeferred<Result<List<TagCategory>>>>()
    var tagCalls = 0
    var categoryCalls = 0

    fun queue() {
        tags.addLast(CompletableDeferred())
        categories.addLast(CompletableDeferred())
    }

    override suspend fun getTags(): Result<List<Tag>> {
        tagCalls++
        return tags.first().await()
    }

    override suspend fun getTagCategories(): Result<List<TagCategory>> {
        categoryCalls++
        return categories.first().await()
    }

    /** The answer of [getItemTags], given at once. */
    var itemTags: Result<List<Tag>> = Result.success(emptyList())
    val itemTagCalls = mutableListOf<String>()

    override suspend fun getItemTags(itemId: String): Result<List<Tag>> {
        itemTagCalls += itemId
        return itemTags
    }

    override suspend fun createTag(label: String): Result<Tag> = error("not used")

    override suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit> = error("not used")
}

/**
 * Only [getCollection] is used: the add form reads its collection's default template. [answer] is
 * given at once; by default the collection has no default template.
 */
internal class FakeCollectionsRepository : CollectionsRepository {
    var answer: Result<Collection> = Result.success(collectionWith(defaultTemplateId = null))
    val getCalls = mutableListOf<String>()

    override suspend fun getCollection(id: String): Result<Collection> {
        getCalls += id
        return answer
    }

    override suspend fun getCollections(): Result<List<Collection>> = error("not used")

    override suspend fun getChildren(id: String): Result<List<Collection>> = error("not used")

    override suspend fun countItems(id: String): Result<Int> = error("not used")

    override suspend fun createCollection(title: String, parentId: String?): Result<Collection> = error("not used")

    override suspend fun renameCollection(id: String, title: String): Result<Collection> = error("not used")

    override suspend fun uploadCollectionImage(
        id: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Collection> = error("not used")

    override suspend fun setDefaultTemplate(id: String, templateId: String?): Result<Collection> = error("not used")

    override suspend fun deleteCollectionImage(id: String): Result<Collection> = error("not used")

    override suspend fun deleteCollection(id: String): Result<Unit> = error("not used")

    companion object {
        fun collectionWith(defaultTemplateId: String?) =
            Collection("books", "Books", parentId = null, color = null, imageUrl = null, defaultTemplateId)
    }
}

/** Records each call; [answer] decides the result and may report progress first. */
internal class FakeAddItemUseCase : AddItemUseCase {
    val calls = mutableListOf<Pair<NewItem, AddItemState>>()
    var answer: suspend (NewItem, AddItemState, (AddItemProgress) -> Unit) -> AddItemResult =
        { _, _, _ -> AddItemResult.Success(NEW_ITEM_ID) }

    override suspend fun addItem(
        newItem: NewItem,
        state: AddItemState,
        onProgress: (AddItemProgress) -> Unit
    ): AddItemResult {
        calls += newItem to state
        return answer(newItem, state, onProgress)
    }

    companion object {
        const val NEW_ITEM_ID = "item-new"
    }
}

/** A photo's bytes are `"<uri>@<rotation>"`, so a test can see which photo went where. */
internal class FakePhotoFiles : PhotoFiles {
    val prepared = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    val unreadable = mutableSetOf<String>()

    override suspend fun prepare(uri: String, rotation: Rotation): ByteArray {
        if (uri in unreadable) error("Cannot read $uri")
        prepared += uri
        return "$uri@$rotation".encodeToByteArray()
    }

    override fun delete(uris: List<String>) {
        deleted += uris
    }
}

/**
 * The reads only: [answer] (the choice lists), [templates] and [fields] (per template id) are
 * given at once to each read.
 */
internal class FakeChoiceLists : TemplatesRepository {
    var answer: Result<List<ChoiceList>> = Result.success(listOf(GENRES, FORMATS))
    var calls = 0

    override suspend fun getChoiceLists(): Result<List<ChoiceList>> {
        calls++
        return answer
    }

    var templates: Result<List<Template>> = Result.success(emptyList())
    var templateCalls = 0
    var fields: Map<String, Result<List<TemplateField>>> = emptyMap()
    val fieldCalls = mutableListOf<String>()

    override suspend fun getTemplates(): Result<List<Template>> {
        templateCalls++
        return templates
    }

    override suspend fun getTemplate(id: String): Result<Template> = error("not used")

    override suspend fun createTemplate(name: String): Result<Template> = error("not used")

    override suspend fun renameTemplate(id: String, name: String): Result<Template> = error("not used")

    override suspend fun deleteTemplate(id: String): Result<Unit> = error("not used")

    override suspend fun getFields(templateId: String): Result<List<TemplateField>> {
        fieldCalls += templateId
        return fields.getValue(templateId)
    }

    override suspend fun createField(
        templateId: String,
        name: String,
        position: Int,
        type: String,
        choiceListId: String?
    ): Result<TemplateField> = error("not used")

    override suspend fun editField(
        id: String,
        name: String?,
        position: Int?,
        type: String?,
        choiceListId: String?
    ): Result<TemplateField> = error("not used")

    override suspend fun deleteField(id: String): Result<Unit> = error("not used")

    override suspend fun createChoiceList(name: String, choices: List<String>): Result<ChoiceList> = error("not used")

    override suspend fun editChoiceList(id: String, name: String, choices: List<String>): Result<ChoiceList> =
        error("not used")

    override suspend fun deleteChoiceList(id: String): Result<Unit> = error("not used")

    companion object {
        val FORMATS = ChoiceList("formats", "Format", listOf("Hardcover", "Paperback", "Ebook"))
        val GENRES = ChoiceList("genres", "genre", listOf("Novel", "Poetry"))
    }
}
