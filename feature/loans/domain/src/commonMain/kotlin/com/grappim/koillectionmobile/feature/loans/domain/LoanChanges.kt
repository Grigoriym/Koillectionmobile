package com.grappim.koillectionmobile.feature.loans.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tells the loans section of an item that a loan of that item changed in this process (a form
 * saved it), so it reads again. One instance per app (bound in `feature:loans:data`). A section
 * that is not collecting misses the signal; it reads when it starts anyway.
 */
class LoanChanges {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** The id of the item whose loans changed. */
    val changed: SharedFlow<String> = changes.asSharedFlow()

    fun notifyChanged(itemId: String) {
        changes.tryEmit(itemId)
    }

    private companion object {
        const val BUFFER = 8
    }
}
