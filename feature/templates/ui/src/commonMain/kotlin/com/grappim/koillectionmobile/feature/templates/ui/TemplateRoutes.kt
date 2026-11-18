package com.grappim.koillectionmobile.feature.templates.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The routes this module owns. Each one is registered in the shell's `navKeySerializersModule`:
 * a route missing from there only breaks back-stack restore after process death.
 */
@Serializable
data object TemplateListRoute : NavKey

/** The fields of template [templateId]. [name] is the screen title, as the template list showed it. */
@Serializable
data class TemplateRoute(val templateId: String, val name: String) : NavKey

/** The template form: a new template when [templateId] is null, else the edit form of that template. */
@Serializable
data class TemplateFormRoute(val templateId: String? = null) : NavKey
