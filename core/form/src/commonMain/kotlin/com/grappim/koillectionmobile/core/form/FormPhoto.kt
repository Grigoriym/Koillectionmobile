package com.grappim.koillectionmobile.core.form

import com.grappim.koillectionmobile.core.image.Rotation
import kotlinx.serialization.Serializable

/** The form's image: a content [uri] and the user's extra [rotation] on top of the EXIF orientation. */
@Serializable
data class FormPhoto(val uri: String, val rotation: Rotation = Rotation.NONE)
