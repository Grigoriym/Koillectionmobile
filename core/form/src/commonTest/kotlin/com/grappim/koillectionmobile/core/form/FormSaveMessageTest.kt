package com.grappim.koillectionmobile.core.form

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.domain.KoillectionError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_http
import com.grappim.koillectionmobile.strings.generated.resources.collection_form_save_error_quota
import kotlin.test.Test
import kotlin.test.assertEquals

class FormSaveMessageTest {

    @Test
    fun `an upload 500 names the quota`() {
        assertEquals(
            NativeText.Resource(RString.collection_form_save_error_quota),
            KoillectionError.Server(500, null).toSaveMessage(uploadFailed = true)
        )
    }

    @Test
    fun `a 500 that is not an upload shows the status`() {
        assertEquals(
            NativeText.Arguments(RString.collection_form_save_error_http, listOf(500)),
            KoillectionError.Server(500, null).toSaveMessage(uploadFailed = false)
        )
    }
}
