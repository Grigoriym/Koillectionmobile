package com.grappim.koillectionmobile.feature.templates.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a template screen that is still on the back stack that a template changed in this process
 * (created, edited or deleted), so it reads it again. One instance per app (bound in
 * `feature:templates:data`). A screen that is not collecting misses the signal; it reads the
 * template when it starts anyway.
 */
class TemplateChanges {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** The ids of templates that changed. */
    val changedTemplates: SharedFlow<String> = changes.asSharedFlow()

    fun notifyChanged(templateId: String) {
        changes.tryEmit(templateId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
