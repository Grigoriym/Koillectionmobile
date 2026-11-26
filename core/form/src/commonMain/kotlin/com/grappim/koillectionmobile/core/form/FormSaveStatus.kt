package com.grappim.koillectionmobile.core.form

import com.grappim.kit.uikit.NativeText

/** The save, once it has started. While it is set, the screen shows it instead of the form. */
sealed interface FormSaveStatus {
    /** The photo is being turned into upload bytes. */
    data object Preparing : FormSaveStatus

    /** [done] of [total] server calls are finished. */
    data class Sending(val done: Int, val total: Int) : FormSaveStatus

    /**
     * The save stopped. [isPartial]: a call went through, so only a retry (which sends the rest) is
     * offered, not a return to the form.
     */
    data class Failed(val message: NativeText, val isPartial: Boolean) : FormSaveStatus
}
