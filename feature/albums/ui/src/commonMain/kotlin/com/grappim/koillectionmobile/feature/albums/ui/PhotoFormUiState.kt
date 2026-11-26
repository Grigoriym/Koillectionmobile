package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import com.grappim.koillectionmobile.feature.albums.domain.PhotoField
import kotlinx.serialization.Serializable

/**
 * The new-photo form: the text [values] and the [photo]. A new photo needs an image before Save
 * (plan §5 decision 14), although the API takes one without. [isSaved] is set once the photo and
 * its image are on the server: the screen then leaves.
 */
data class NewPhotoUiState(
    val values: PhotoFormValues = PhotoFormValues(),
    val photo: FormPhoto? = null,
    val save: FormSaveStatus? = null,
    val isSaved: Boolean = false,
    val onFieldChange: (PhotoField, String) -> Unit = { _, _ -> },
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The user entered something, so leaving the form asks first. */
    val hasInput: Boolean
        get() = values != PhotoFormValues() || photo != null

    val canSave: Boolean
        get() = values.hasTitle && photo != null
}

/** The photo as the edit form loaded it: the base of the save's diff. */
@Serializable
data class LoadedPhoto(val values: PhotoFormValues, val albumId: String, val imageUrl: String?)

/** One server call of an edit save, in the order the save sends them. */
@Serializable
enum class PhotoEditStep { EDIT, UPLOAD_IMAGE }

/**
 * The edit form of a photo: the new-photo form, filled from the server ([original]). [LoadedPhoto.imageUrl]
 * is the server image. A new [photo] replaces it; the server image cannot be removed (`Photo` has no
 * `deleteImage` in Koillection 1.8.5, plan §5 decision 14).
 *
 * While [isLoading] or with an [error], the form is not shown. [isSaved] is set once every change
 * is on the server: the screen then leaves.
 */
data class EditPhotoUiState(
    val original: LoadedPhoto? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val values: PhotoFormValues = PhotoFormValues(),
    val photo: FormPhoto? = null,
    val save: FormSaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onFieldChange: (PhotoField, String) -> Unit = { _, _ -> },
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The calls a save sends: one `PATCH` of the changed fields, then the image upload. */
    val changes: List<PhotoEditStep>
        get() {
            val loaded = original ?: return emptyList()
            return buildList {
                if (values.changesFrom(loaded.values).isNotEmpty()) add(PhotoEditStep.EDIT)
                if (photo != null) add(PhotoEditStep.UPLOAD_IMAGE)
            }
        }

    /** A save without a change sends nothing. */
    val canSave: Boolean
        get() = values.hasTitle && changes.isNotEmpty()
}
