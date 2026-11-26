package com.grappim.koillectionmobile.feature.tags.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.core.ui.toMessage
import com.grappim.koillectionmobile.feature.tags.domain.TagsRepository
import com.grappim.koillectionmobile.feature.tags.domain.loadTagGroups
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.tags_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Reads the tags and the tag categories once when the screen opens, and again on retry. Opening and closing a group
 * stays in this ViewModel, so it lasts while the user visits a tag's items and comes back.
 */
@KoinViewModel
class TagListViewModel(private val tagsRepository: TagsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TagListUiState(onToggleGroup = ::toggleGroup, onRetry = ::retry))
    val uiState: StateFlow<TagListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun toggleGroup(index: Int) {
        _uiState.update { state ->
            val expanded = state.expandedGroups
            state.copy(expandedGroups = if (index in expanded) expanded - index else expanded + index)
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = tagsRepository.loadTagGroups()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { groups ->
                        state.copy(
                            groups = groups,
                            expandedGroups = if (groups.size == 1) setOf(0) else emptySet(),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            groups = emptyList(),
                            expandedGroups = emptySet(),
                            isLoading = false,
                            error = it.toMessage(RString.tags_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
