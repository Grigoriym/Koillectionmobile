package com.grappim.koillectionmobile.feature.wishlists.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import kotlinx.serialization.Serializable

/**
 * The new-wishlist form: the [name] and an optional [photo] (plan §5 decision 12). [isSaved] is set
 * once the wishlist and its image are on the server: the screen then leaves.
 */
data class NewWishlistUiState(
    val name: String = "",
    val photo: FormPhoto? = null,
    val save: FormSaveStatus? = null,
    val isSaved: Boolean = false,
    val onNameChange: (String) -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The user entered something, so leaving the form asks first. */
    val hasInput: Boolean
        get() = name.isNotBlank() || photo != null

    /** The server needs a name (`NotBlank` in `Wishlist.php`). */
    val canSave: Boolean
        get() = name.isNotBlank()
}

/** The wishlist as the edit form loaded it: the base of the save's diff. */
@Serializable
data class LoadedWishlist(val name: String, val parentId: String?, val imageUrl: String?)

/** One server call of an edit save, in the order the save sends them. */
@Serializable
enum class WishlistEditStep { RENAME, UPLOAD_IMAGE, DELETE_IMAGE }

/**
 * The edit form of a wishlist: the new-wishlist form, filled from the server ([original]).
 * [imageUrl] is the server image the form keeps, or null once the user removed it. A new [photo]
 * replaces it.
 *
 * While [isLoading] or with an [error], the form is not shown. [isSaved] is set once every change
 * is on the server: the screen then leaves.
 */
data class EditWishlistUiState(
    val original: LoadedWishlist? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val name: String = "",
    val photo: FormPhoto? = null,
    val imageUrl: String? = null,
    val save: FormSaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The calls a save sends: only what differs from [original]. */
    val changes: List<WishlistEditStep>
        get() {
            val loaded = original ?: return emptyList()
            return buildList {
                if (name.trim() != loaded.name) add(WishlistEditStep.RENAME)
                if (photo != null) {
                    add(WishlistEditStep.UPLOAD_IMAGE)
                } else if (loaded.imageUrl != null && imageUrl == null) {
                    add(WishlistEditStep.DELETE_IMAGE)
                }
            }
        }

    /** The server needs a name (`NotBlank` in `Wishlist.php`), and a save without a change sends nothing. */
    val canSave: Boolean
        get() = name.isNotBlank() && changes.isNotEmpty()
}
