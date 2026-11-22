package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.wishlists.domain.WishField
import kotlinx.serialization.Serializable

/**
 * The new-wish form: the text [values] and an optional [photo] (plan §5 decision 12). [isSaved] is
 * set once the wish and its image are on the server: the screen then leaves.
 */
data class NewWishUiState(
    val values: WishFormValues = WishFormValues(),
    val photo: FormPhoto? = null,
    val save: WishlistSaveStatus? = null,
    val isSaved: Boolean = false,
    val onFieldChange: (WishField, String) -> Unit = { _, _ -> },
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The user entered something, so leaving the form asks first. */
    val hasInput: Boolean
        get() = values != WishFormValues() || photo != null

    val canSave: Boolean
        get() = values.canSave
}

/** The wish as the edit form loaded it: the base of the save's diff. */
@Serializable
data class LoadedWish(val values: WishFormValues, val wishlistId: String, val imageUrl: String?)

/** One server call of an edit save, in the order the save sends them. */
@Serializable
enum class WishEditStep { EDIT, UPLOAD_IMAGE }

/**
 * The edit form of a wish: the new-wish form, filled from the server ([original]). [imageUrl] is
 * the server image. A new [photo] replaces it; the server image cannot be removed (`Wish` has no
 * `deleteImage` in Koillection 1.8.5, plan §5 decision 12).
 *
 * While [isLoading] or with an [error], the form is not shown. [isSaved] is set once every change
 * is on the server: the screen then leaves.
 */
data class EditWishUiState(
    val original: LoadedWish? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val values: WishFormValues = WishFormValues(),
    val photo: FormPhoto? = null,
    val save: WishlistSaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onFieldChange: (WishField, String) -> Unit = { _, _ -> },
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The calls a save sends: one `PATCH` of the changed fields, then the image upload. */
    val changes: List<WishEditStep>
        get() {
            val loaded = original ?: return emptyList()
            return buildList {
                if (values.changesFrom(loaded.values).isNotEmpty()) add(WishEditStep.EDIT)
                if (photo != null) add(WishEditStep.UPLOAD_IMAGE)
            }
        }

    /** A save without a change sends nothing. */
    val canSave: Boolean
        get() = values.canSave && changes.isNotEmpty()
}
