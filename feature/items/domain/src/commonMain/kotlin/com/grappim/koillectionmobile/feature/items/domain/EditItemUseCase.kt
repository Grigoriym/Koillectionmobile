package com.grappim.koillectionmobile.feature.items.domain

import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.tags.domain.findOrCreateTags

/** An item with its fields (sorted by position) and the ids of its tags, as read from the server. */
data class ItemSnapshot(val item: Item, val data: List<Datum>, val tagIds: Set<String>)

/**
 * The edit form's values for an item. [fields] holds each existing `text` and `image` field that
 * stays: an existing field of those types that is not in [fields] is deleted. Fields of other
 * types are never touched, whether they are in [fields] or not. [newMainImage] replaces the main
 * image; null keeps it (the API cannot remove a main image). [tagIds] are existing tags;
 * [newTagLabels] are tags to create (or to find, see `findOrCreateTags`) before the tags are set.
 */
data class ItemEdit(
    val name: String,
    val fields: List<EditedField>,
    val newTextFields: List<NewTextField>,
    val newImageFields: List<NewImageField>,
    val newMainImage: NewImage?,
    val tagIds: List<String>,
    val newTagLabels: List<String> = emptyList()
)

/** An existing field as the form has it. [datumId] is the server id of the field. */
sealed interface EditedField {
    val datumId: String
    val label: String

    data class Text(override val datumId: String, override val label: String, val value: String) : EditedField

    /** [newImage] replaces the field's image; null keeps it. */
    data class Image(override val datumId: String, override val label: String, val newImage: NewImage?) :
        EditedField
}

/** [done] of [total] server calls are finished. */
data class EditItemProgress(val done: Int, val total: Int)

sealed interface EditItemResult {
    data object Success : EditItemResult

    /** No call was sent: two fields would have [label] (labels are unique within an item, server rule). */
    data class DuplicateLabel(val label: String) : EditItemResult

    /**
     * Some calls may have gone through: retry with [EditItemUseCase.retry]. [isUpload]: the failed
     * call was an image upload (the main image or a field image).
     */
    data class Failure(val error: Throwable, val isUpload: Boolean = false) : EditItemResult
}

interface EditItemUseCase {

    /**
     * Sends what [edit] changes in [original] (the item as the form loaded it). Order: field
     * deletes (a freed label can be reused), the rename, field label and value changes, field
     * image replacements, new text fields, new image fields (created, then the image uploaded),
     * the main image, the tags (only if the set changed or there are new tags). New tags are found
     * or created just before the tags are set. New fields get positions after the last
     * field that stays. A label swap between fields goes through a temporary label.
     */
    suspend fun editItem(
        original: ItemSnapshot,
        edit: ItemEdit,
        onProgress: (EditItemProgress) -> Unit = {}
    ): EditItemResult

    /**
     * As [editItem], after a failure: reads the item from the server again and sends only what
     * still differs, so a call that already went through is not sent twice.
     */
    suspend fun retry(
        original: ItemSnapshot,
        edit: ItemEdit,
        onProgress: (EditItemProgress) -> Unit = {}
    ): EditItemResult
}

class EditItemUseCaseImpl(private val itemsRepository: ItemsRepository, private val tagsRepository: TagsRepository) :
    EditItemUseCase {

    override suspend fun editItem(
        original: ItemSnapshot,
        edit: ItemEdit,
        onProgress: (EditItemProgress) -> Unit
    ): EditItemResult = send(original, original, edit, onProgress)

    override suspend fun retry(
        original: ItemSnapshot,
        edit: ItemEdit,
        onProgress: (EditItemProgress) -> Unit
    ): EditItemResult {
        val id = original.item.id
        val current = runCatching {
            ItemSnapshot(
                item = itemsRepository.getItem(id).getOrThrow(),
                data = itemsRepository.getData(id).getOrThrow(),
                tagIds = tagsRepository.getItemTags(id).getOrThrow().mapTo(mutableSetOf()) { it.id }
            )
        }.getOrElse { return EditItemResult.Failure(it) }
        return send(original, current, edit, onProgress)
    }

    private suspend fun send(
        original: ItemSnapshot,
        current: ItemSnapshot,
        edit: ItemEdit,
        onProgress: (EditItemProgress) -> Unit
    ): EditItemResult {
        val calls = when (val plan = plan(original, current, edit)) {
            is Plan.Conflict -> return EditItemResult.DuplicateLabel(plan.label)
            is Plan.Calls -> plan.calls
        }
        var done = 0
        val total = calls.sumOf { it.size }
        onProgress(EditItemProgress(done, total))
        for (call in calls) {
            run(original.item.id, call) {
                done++
                onProgress(EditItemProgress(done, total))
            }?.let { return it }
        }
        return EditItemResult.Success
    }

    /** Sends [call]. Returns null on success. */
    private suspend fun run(itemId: String, call: Call, onCallDone: () -> Unit): EditItemResult.Failure? {
        val result: Result<Any> = when (call) {
            is Call.DeleteField -> itemsRepository.deleteDatum(call.datumId)

            is Call.Rename -> itemsRepository.renameItem(itemId, call.name)

            is Call.PatchField -> itemsRepository.updateDatum(call.datumId, call.label, call.value)

            is Call.UploadFieldImage -> upload(call.datumId, call.image)

            is Call.CreateTextField ->
                itemsRepository.createTextDatum(itemId, call.field.label, call.field.value, call.position)

            is Call.CreateImageField -> {
                val datum = itemsRepository.createImageDatum(itemId, call.field.label, call.position)
                    .getOrElse { return EditItemResult.Failure(it) }
                onCallDone()
                upload(datum.id, call.field.image)
            }

            is Call.UploadMainImage ->
                itemsRepository.uploadItemImage(itemId, call.image.bytes, call.image.fileName, call.image.mimeType)

            is Call.SetTags -> {
                val newTagIds = if (call.newTagLabels.isEmpty()) {
                    emptyList()
                } else {
                    tagsRepository.findOrCreateTags(call.newTagLabels)
                        .getOrElse { return EditItemResult.Failure(it) }
                }
                tagsRepository.setItemTags(itemId, (call.tagIds + newTagIds).distinct())
            }
        }
        return result.fold(
            onSuccess = {
                onCallDone()
                null
            },
            onFailure = { EditItemResult.Failure(it, isUpload = call.isUpload) }
        )
    }

    private suspend fun upload(datumId: String, image: NewImage) =
        itemsRepository.uploadDatumImage(datumId, image.bytes, image.fileName, image.mimeType)

    /** Works out the calls that take [current] (the server now) to [edit]. [original] is what the form loaded. */
    private fun plan(original: ItemSnapshot, current: ItemSnapshot, edit: ItemEdit): Plan {
        val originalIds = original.data.mapTo(mutableSetOf()) { it.id }
        val edited = edit.fields
            .filter { field -> original.data.any { it.id == field.datumId && it.type == field.type } }
            .associateBy { it.datumId }
        val keptIds = edit.fields.mapTo(mutableSetOf()) { it.datumId }
        val deletedIds = original.data.filter {
            it.type in EDITABLE_TYPES && it.id !in keptIds
        }.mapTo(mutableSetOf()) { it.id }

        val staying = current.data.filter { it.id !in deletedIds }
        // A field that is not in the original but has a new field's label was created by an earlier try.
        val created = current.data.filter { it.id !in originalIds }.associateBy { it.label }
        val newImageFields = edit.newImageFields

        val finalLabels = staying.map { (edited[it.id]?.label ?: it.label).orEmpty() } +
            (edit.newTextFields.map { it.label } + newImageFields.map { it.label }).filter { it !in created }
        finalLabels.groupBy { it }.entries.firstOrNull { it.value.size > 1 }?.let { return Plan.Conflict(it.key) }

        val calls = mutableListOf<Call>()
        current.data.filter { it.id in deletedIds }.forEach { calls += Call.DeleteField(it.id) }
        if (edit.name != current.item.name) calls += Call.Rename(edit.name)
        calls += patches(staying, edited)
        // A field whose image URL is still the original one has not got its new image yet.
        val originalUrls = original.data.associate { it.id to it.image?.url }
        staying.forEach { datum ->
            val image = (edited[datum.id] as? EditedField.Image)?.newImage
            if (image != null && datum.image?.url == originalUrls[datum.id]) {
                calls += Call.UploadFieldImage(datum.id, image)
            }
        }

        // Positions come from the original, so a retry gives a new field the same position.
        val lastPosition = original.data.filter { it.id !in deletedIds }.mapNotNull { it.position }.maxOrNull() ?: 0
        edit.newTextFields.forEachIndexed { index, field ->
            if (field.label !in created) calls += Call.CreateTextField(field, lastPosition + index + 1)
        }
        newImageFields.forEachIndexed { index, field ->
            val datum = created[field.label]
            when {
                datum == null ->
                    calls += Call.CreateImageField(field, lastPosition + edit.newTextFields.size + index + 1)

                datum.image == null -> calls += Call.UploadFieldImage(datum.id, field.image)
            }
        }

        val mainImage = edit.newMainImage
        if (mainImage != null && current.item.image?.url == original.item.image?.url) {
            calls += Call.UploadMainImage(mainImage)
        }
        if (edit.newTagLabels.isNotEmpty() || edit.tagIds.toSet() != current.tagIds) {
            calls += Call.SetTags(edit.tagIds, edit.newTagLabels)
        }
        return Plan.Calls(calls)
    }

    /**
     * The label and value changes of [edited] fields, in an order where no `PATCH` gives a field a
     * label another field still has (the server answers that with an empty 500, plan §2). A cycle
     * (a swap) is broken with a temporary label. [staying] must have unique final labels.
     */
    private fun patches(staying: List<Datum>, edited: Map<String, EditedField>): List<Call.PatchField> {
        val labels = staying.associate { it.id to it.label.orEmpty() }.toMutableMap()
        val pending = staying.mapNotNull { datum ->
            val field = edited[datum.id] ?: return@mapNotNull null
            val label = field.label.takeIf { it != datum.label }
            val value = (field as? EditedField.Text)?.value?.takeIf { it != datum.value }
            if (label == null && value == null) null else Pending(datum.id, label, value)
        }.toMutableList()

        val calls = mutableListOf<Call.PatchField>()
        while (pending.isNotEmpty()) {
            val free = pending.firstOrNull { p ->
                p.label == null ||
                    labels.none { it.key != p.id && it.value == p.label }
            }
            if (free != null) {
                pending -= free
                calls += Call.PatchField(free.id, free.label, free.value)
                free.label?.let { labels[free.id] = it }
            } else {
                // Every waiting field wants a label another waiting field has: move one out of the way.
                val blocked = pending.first()
                val inUse = labels.values + pending.mapNotNull { it.label }
                val temporary = generateSequence(1) { it + 1 }
                    .map { "${labels.getValue(blocked.id)} ($TEMPORARY_LABEL $it)" }
                    .first { it !in inUse }
                calls += Call.PatchField(blocked.id, temporary, null)
                labels[blocked.id] = temporary
            }
        }
        return calls
    }

    private val EditedField.type: String
        get() = when (this) {
            is EditedField.Text -> TYPE_TEXT
            is EditedField.Image -> TYPE_IMAGE
        }

    private class Pending(val id: String, val label: String?, val value: String?)

    private sealed interface Plan {
        class Calls(val calls: List<Call>) : Plan

        class Conflict(val label: String) : Plan
    }

    private sealed interface Call {
        /** The number of server calls, for the progress. */
        val size: Int get() = 1

        /** The call (its last part, for [CreateImageField]) is an image upload. */
        val isUpload: Boolean get() = false

        class DeleteField(val datumId: String) : Call

        class Rename(val name: String) : Call

        class PatchField(val datumId: String, val label: String?, val value: String?) : Call

        class UploadFieldImage(val datumId: String, val image: NewImage) : Call {
            override val isUpload: Boolean get() = true
        }

        class CreateTextField(val field: NewTextField, val position: Int) : Call

        class CreateImageField(val field: NewImageField, val position: Int) : Call {
            override val size: Int get() = 2
            override val isUpload: Boolean get() = true
        }

        class UploadMainImage(val image: NewImage) : Call {
            override val isUpload: Boolean get() = true
        }

        /** [newTagLabels] are found or created first; that counts as part of this one call. */
        class SetTags(val tagIds: List<String>, val newTagLabels: List<String>) : Call
    }

    private companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        val EDITABLE_TYPES = setOf(TYPE_TEXT, TYPE_IMAGE)

        /** Part of a temporary label during a swap. The label is visible on the server if the save stops there. */
        const val TEMPORARY_LABEL = "moving"
    }
}
