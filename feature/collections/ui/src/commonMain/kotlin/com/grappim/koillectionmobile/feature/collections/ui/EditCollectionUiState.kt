package com.grappim.koillectionmobile.feature.collections.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.templates.domain.Template
import kotlinx.serialization.Serializable

/** The collection as the edit form loaded it: the base of the save's diff. */
@Serializable
data class LoadedCollection(
    val title: String,
    val parentId: String?,
    val imageUrl: String?,
    val defaultTemplateId: String? = null
)

/** One server call of an edit save, in the order the save sends them. */
@Serializable
enum class CollectionEditStep { RENAME, SET_DEFAULT_TEMPLATE, UPLOAD_IMAGE, DELETE_IMAGE }

/**
 * The server's templates, for the default-template picker. [error] is set when the load failed;
 * the rest of the form still works.
 */
data class TemplatesUi(
    val templates: List<Template> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty
)

/**
 * The edit form of a collection: the new-collection form (plan §5 decision 9), filled from the
 * server ([original]). [imageUrl] is the server image the form keeps, or null once the user removed
 * it. A new [photo] replaces it. [defaultTemplateId] is the template the add form in this collection
 * applies when it opens, or null for none; the picker offers [templates].
 *
 * While [isLoading] or with an [error], the form is not shown. [isSaved] is set once every change
 * is on the server: the screen then leaves.
 */
data class EditCollectionUiState(
    val original: LoadedCollection? = null,
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val title: String = "",
    val photo: FormPhoto? = null,
    val imageUrl: String? = null,
    val defaultTemplateId: String? = null,
    val templates: TemplatesUi = TemplatesUi(),
    val save: CollectionSaveStatus? = null,
    val isSaved: Boolean = false,
    val onRetry: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onPhotosAdd: (List<String>) -> Unit = {},
    val onPhotoRotate: () -> Unit = {},
    val onPhotoRemove: () -> Unit = {},
    val onDefaultTemplateChange: (String?) -> Unit = {},
    val onRetryTemplates: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onEditForm: () -> Unit = {}
) {
    /** The calls a save sends: only what differs from [original]. */
    val changes: List<CollectionEditStep>
        get() {
            val loaded = original ?: return emptyList()
            return buildList {
                if (title.trim() != loaded.title) add(CollectionEditStep.RENAME)
                if (defaultTemplateId != loaded.defaultTemplateId) add(CollectionEditStep.SET_DEFAULT_TEMPLATE)
                if (photo != null) {
                    add(CollectionEditStep.UPLOAD_IMAGE)
                } else if (loaded.imageUrl != null && imageUrl == null) {
                    add(CollectionEditStep.DELETE_IMAGE)
                }
            }
        }

    /** The server needs a title (`NotBlank` in `Collection.php`), and a save without a change sends nothing. */
    val canSave: Boolean
        get() = title.isNotBlank() && changes.isNotEmpty()
}
