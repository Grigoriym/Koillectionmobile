package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.image.Rotation
import kotlinx.serialization.Serializable

/** The form's image: a content [uri] and the user's extra [rotation] on top of the EXIF orientation. */
@Serializable
data class FormPhoto(val uri: String, val rotation: Rotation = Rotation.NONE)

/** The save, once it has started. While it is set, the screen shows it instead of the form. */
sealed interface CollectionSaveStatus {
    /** The photo is being turned into upload bytes. */
    data object Preparing : CollectionSaveStatus

    /** [done] of [total] server calls are finished. */
    data class Sending(val done: Int, val total: Int) : CollectionSaveStatus

    /**
     * The save stopped. [isPartial]: the collection exists on the server, so only a retry (which
     * sends the image) is offered, not a return to the form.
     */
    data class Failed(val message: NativeText, val isPartial: Boolean) : CollectionSaveStatus
}

/**
 * The new-collection form: the [title] and an optional [photo] (plan §5 decision 9).
 * [savedCollectionId] is set once the collection is saved: the screen then opens it.
 */
data class NewCollectionUiState(
    val title: String = "",
    val photo: FormPhoto? = null,
    val save: CollectionSaveStatus? = null,
    val savedCollectionId: String? = null,
    val onTitleChange: (String) -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The user entered something, so leaving the form asks first. */
    val hasInput: Boolean
        get() = title.isNotBlank() || photo != null

    /** The server needs a title (`NotBlank` in `Collection.php`). */
    val canSave: Boolean
        get() = title.isNotBlank()
}
