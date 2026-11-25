package com.grappim.koillectionmobile.feature.logs.ui

import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.logs.domain.LogEntry

/**
 * The logs, newest first. [isLoading] and [error] are for the first page: a failed first page
 * clears [logs] and sets [error], and [onRetry] reads it again.
 *
 * [onLoadMore] reads the next page when the user scrolls near the end. It does nothing while a
 * page loads, after the oldest page, or while [moreError] is set. A failed later page keeps
 * [logs] and sets [moreError]; [onRetryMore] reads that page again.
 */
data class LogListUiState(
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: NativeText = NativeText.Empty,
    val isLoadingMore: Boolean = false,
    val moreError: NativeText = NativeText.Empty,
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetryMore: () -> Unit = {}
)
