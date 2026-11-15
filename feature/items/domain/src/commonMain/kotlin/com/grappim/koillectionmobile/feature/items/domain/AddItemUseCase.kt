package com.grappim.koillectionmobile.feature.items.domain

import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository

/** An item to create in collection [collectionId]. Labels must be unique within the item (server rule). */
data class NewItem(
    val collectionId: String,
    val name: String,
    val textFields: List<NewTextField>,
    val mainImage: NewImage?,
    val imageFields: List<NewImageField>,
    val tagIds: List<String>
)

data class NewTextField(val label: String, val value: String)

data class NewImageField(val label: String, val image: NewImage)

/** A prepared image (M3.2's output). [mimeType] is one the server takes, see [ItemsRepository.uploadItemImage]. */
class NewImage(val bytes: ByteArray, val fileName: String, val mimeType: String)

/**
 * What of a [NewItem] exists on the server. A failed [AddItemUseCase.addItem] returns it, and a
 * retry with the same [NewItem] and this state sends only the rest.
 *
 * [imageFieldIds] holds the field id of each image field created so far, in order;
 * [imageFieldsUploaded] counts the ones whose image is uploaded too.
 */
data class AddItemState(
    val itemId: String? = null,
    val textFieldsDone: Int = 0,
    val mainImageDone: Boolean = false,
    val imageFieldIds: List<String> = emptyList(),
    val imageFieldsUploaded: Int = 0,
    val tagsDone: Boolean = false
)

/**
 * [done] of [total] server calls are finished. [state] is what exists on the server at this point:
 * keep it, so a retry after a process death does not create a second item.
 */
data class AddItemProgress(val done: Int, val total: Int, val state: AddItemState)

sealed interface AddItemResult {
    data class Success(val itemId: String) : AddItemResult

    /** [state] is what exists on the server: pass it to the retry. */
    data class Failure(val error: Throwable, val state: AddItemState) : AddItemResult
}

interface AddItemUseCase {

    /**
     * Creates [newItem] in this order: the item, the text fields, the main image, each image field
     * (created, then its image uploaded), the tags. Positions: text fields from 1, then the image
     * fields. Starts from [state], so a retry never creates a second item or a field twice.
     */
    suspend fun addItem(
        newItem: NewItem,
        state: AddItemState = AddItemState(),
        onProgress: (AddItemProgress) -> Unit = {}
    ): AddItemResult
}

class AddItemUseCaseImpl(private val itemsRepository: ItemsRepository, private val tagsRepository: TagsRepository) :
    AddItemUseCase {

    override suspend fun addItem(
        newItem: NewItem,
        state: AddItemState,
        onProgress: (AddItemProgress) -> Unit
    ): AddItemResult {
        var current = state
        val total = totalCalls(newItem)
        fun report() = onProgress(AddItemProgress(callsDone(current, newItem), total, current))

        report()
        val itemId = current.itemId ?: itemsRepository.createItem(newItem.collectionId, newItem.name)
            .getOrElse { return AddItemResult.Failure(it, current) }
            .id
        current = current.copy(itemId = itemId)
        report()

        while (current.textFieldsDone < newItem.textFields.size) {
            val index = current.textFieldsDone
            val field = newItem.textFields[index]
            itemsRepository.createTextDatum(itemId, field.label, field.value, position = index + 1)
                .onFailure { return AddItemResult.Failure(it, current) }
            current = current.copy(textFieldsDone = index + 1)
            report()
        }

        val mainImage = newItem.mainImage
        if (mainImage != null && !current.mainImageDone) {
            itemsRepository.uploadItemImage(itemId, mainImage.bytes, mainImage.fileName, mainImage.mimeType)
                .onFailure { return AddItemResult.Failure(it, current) }
            current = current.copy(mainImageDone = true)
            report()
        }

        while (current.imageFieldsUploaded < newItem.imageFields.size) {
            val index = current.imageFieldsUploaded
            val field = newItem.imageFields[index]
            if (current.imageFieldIds.size == index) {
                val position = newItem.textFields.size + index + 1
                val datum = itemsRepository.createImageDatum(itemId, field.label, position)
                    .getOrElse { return AddItemResult.Failure(it, current) }
                current = current.copy(imageFieldIds = current.imageFieldIds + datum.id)
                report()
            }
            val image = field.image
            itemsRepository.uploadDatumImage(current.imageFieldIds[index], image.bytes, image.fileName, image.mimeType)
                .onFailure { return AddItemResult.Failure(it, current) }
            current = current.copy(imageFieldsUploaded = index + 1)
            report()
        }

        // A new item has no tags, so an empty list needs no call.
        if (newItem.tagIds.isNotEmpty() && !current.tagsDone) {
            tagsRepository.setItemTags(itemId, newItem.tagIds)
                .onFailure { return AddItemResult.Failure(it, current) }
            current = current.copy(tagsDone = true)
            report()
        }

        return AddItemResult.Success(itemId)
    }

    private fun totalCalls(newItem: NewItem): Int = 1 +
        newItem.textFields.size +
        (if (newItem.mainImage != null) 1 else 0) +
        2 * newItem.imageFields.size +
        (if (newItem.tagIds.isNotEmpty()) 1 else 0)

    private fun callsDone(state: AddItemState, newItem: NewItem): Int = (if (state.itemId != null) 1 else 0) +
        state.textFieldsDone +
        (if (newItem.mainImage != null && state.mainImageDone) 1 else 0) +
        state.imageFieldIds.size +
        state.imageFieldsUploaded +
        (if (state.tagsDone) 1 else 0)
}
