package com.grappim.koillectionmobile.feature.albums.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.form.FormPhoto
import com.grappim.koillectionmobile.core.form.FormSaveStatus
import kotlinx.serialization.Serializable

/**
 * The new-album form: the [title] and an optional [photo] (plan §5 decision 14). [isSaved] is set
 * once the album and its image are on the server: the screen then leaves.
 */
data class NewAlbumUiState(
    val title: String = "",
    val photo: FormPhoto? = null,
    val save: FormSaveStatus? = null,
    val isSaved: Boolean = false,
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

    /** The server needs a title (`NotBlank` in `Album.php`). */
    val canSave: Boolean
        get() = title.isNotBlank()
}

/** The album as the edit form loaded it: the base of the save's diff. */
@Serializable
data class LoadedAlbum(val title: String, val parentId: String?, val imageUrl: String?)

/** One server call of an edit save, in the order the save sends them. */
@Serializable
enum class AlbumEditStep { RENAME, UPLOAD_IMAGE, DELETE_IMAGE }

/**
 * The edit form of an album: the new-album form, filled from the server ([original]). [imageUrl] is
 * the server image the form keeps, or null once the user removed it. A new [photo] replaces it.
 *
 * While [isLoading] or with an [error], the form is not shown. [isSaved] is set once every change
 * is on the server: the screen then leaves.
 */
data class EditAlbumUiState(
    val original: LoadedAlbum? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val title: String = "",
    val photo: FormPhoto? = null,
    val imageUrl: String? = null,
    val save: FormSaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The calls a save sends: only what differs from [original]. */
    val changes: List<AlbumEditStep>
        get() {
            val loaded = original ?: return emptyList()
            return buildList {
                if (title.trim() != loaded.title) add(AlbumEditStep.RENAME)
                if (photo != null) {
                    add(AlbumEditStep.UPLOAD_IMAGE)
                } else if (loaded.imageUrl != null && imageUrl == null) {
                    add(AlbumEditStep.DELETE_IMAGE)
                }
            }
        }

    /** The server needs a title (`NotBlank` in `Album.php`), and a save without a change sends nothing. */
    val canSave: Boolean
        get() = title.isNotBlank() && changes.isNotEmpty()
}
