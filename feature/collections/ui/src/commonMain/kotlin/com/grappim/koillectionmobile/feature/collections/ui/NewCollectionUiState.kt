package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus

/**
 * The new-collection form: the [title] and an optional [photo] (plan §5 decision 9).
 * [savedCollectionId] is set once the collection is saved: the screen then opens it.
 */
data class NewCollectionUiState(
    val title: String = "",
    val photo: FormPhoto? = null,
    val save: FormSaveStatus? = null,
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
