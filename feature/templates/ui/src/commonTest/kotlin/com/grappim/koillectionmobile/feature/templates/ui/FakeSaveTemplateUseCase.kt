package com.grappim.koillectionmobile.feature.templates.ui

import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateProgress
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateResult
import com.grappim.koillectionmobile.feature.templates.domain.SaveTemplateUseCase
import com.grappim.koillectionmobile.feature.templates.domain.TemplateEdit
import com.grappim.koillectionmobile.feature.templates.domain.TemplateSnapshot
import kotlinx.coroutines.CompletableDeferred

/**
 * Records each call as a [Call]. Each call reports [progress] first, then waits for the first
 * queued result. Queue one before the call.
 */
class FakeSaveTemplateUseCase : SaveTemplateUseCase {
    data class Call(val retryId: String?, val original: TemplateSnapshot?, val edit: TemplateEdit)

    val calls = mutableListOf<Call>()
    val results = ArrayDeque<CompletableDeferred<SaveTemplateResult>>()
    var progress: List<SaveTemplateProgress> = emptyList()

    override suspend fun save(
        original: TemplateSnapshot?,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit
    ): SaveTemplateResult {
        calls += Call(null, original, edit)
        progress.forEach(onProgress)
        return results.removeFirst().await()
    }

    override suspend fun retry(
        templateId: String,
        original: TemplateSnapshot?,
        edit: TemplateEdit,
        onProgress: (SaveTemplateProgress) -> Unit
    ): SaveTemplateResult {
        calls += Call(templateId, original, edit)
        progress.forEach(onProgress)
        return results.removeFirst().await()
    }
}
