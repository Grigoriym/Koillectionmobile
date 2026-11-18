package com.grappim.koillectionmobile.feature.templates.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells a screen that is still on the back stack that a choice list changed in this process
 * (created, edited or deleted), so it reads the choice lists again. One instance per app (bound in
 * `feature:templates:data`). A screen that is not collecting misses the signal; it reads the
 * choice lists when it starts anyway.
 */
class ChoiceListChanges {
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = BUFFER)

    val changed: SharedFlow<Unit> = changes.asSharedFlow()

    fun notifyChanged() {
        changes.tryEmit(Unit)
    }

    private companion object {
        const val BUFFER = 8
    }
}
