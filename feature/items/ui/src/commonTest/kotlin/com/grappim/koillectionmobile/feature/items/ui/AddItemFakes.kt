package com.grappim.koillectionmobile.feature.items.ui

import com.grappim.koillectionmobile.core.image.Rotation
import com.grappim.koillectionmobile.feature.items.domain.AddItemProgress
import com.grappim.koillectionmobile.feature.items.domain.AddItemResult
import com.grappim.koillectionmobile.feature.items.domain.AddItemState
import com.grappim.koillectionmobile.feature.items.domain.AddItemUseCase
import com.grappim.koillectionmobile.feature.items.domain.NewItem
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagCategory
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
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

    override suspend fun getItemTags(itemId: String): Result<List<Tag>> = error("not used")

    override suspend fun setItemTags(itemId: String, tagIds: List<String>): Result<Unit> = error("not used")
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
