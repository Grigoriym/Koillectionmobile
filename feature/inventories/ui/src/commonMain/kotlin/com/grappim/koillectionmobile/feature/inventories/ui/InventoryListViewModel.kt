package com.grappim.koillectionmobile.feature.inventories.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.inventories.domain.InventoriesRepository
import com.grappim.koillectionmobile.feature.inventories.domain.Inventory
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.inventories_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The inventories of the user. The web UI lists them in database order (`ToolsController` calls
 * `findAll()`), which the API does not show, so the app shows the newest first. A delete asks
 * first and reads the list again on success. A delete on the inventory screen
 * ([InventoryChanges]) reads the list again too, while the list waits lower on the back stack.
 */
@KoinViewModel
class InventoryListViewModel(
    private val repository: InventoriesRepository,
    private val inventoryChanges: InventoryChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryListUiState(
            onRetry = ::retry,
            onDeleteClick = ::askDelete,
            onDeleteConfirm = ::delete,
            onDeleteDismiss = ::dismissDelete,
            onDeleteErrorShown = ::clearDeleteError
        )
    )
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            inventoryChanges.deleted.collect { reload() }
        }
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        reload()
    }

    private fun reload() {
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun askDelete(id: String) {
        val state = _uiState.value
        if (state.isLoading || state.deletingId != null) return
        val inventory = state.inventories.find { it.id == id } ?: return
        _uiState.update { it.copy(deleteConfirm = inventory) }
    }

    private fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirm = null) }
    }

    private fun delete() {
        val inventory = _uiState.value.deleteConfirm ?: return
        _uiState.update {
            it.copy(deleteConfirm = null, deletingId = inventory.id, deleteError = NativeText.Empty)
        }
        viewModelScope.launch {
            repository.deleteInventory(inventory.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(deletingId = null, isLoading = true, error = NativeText.Empty)
                    }
                    load()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            deletingId = null,
                            deleteError = e.toMessage(RString.inventories_delete_error_unexpected)
                        )
                    }
                }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }

    private fun load() {
        viewModelScope.launch {
            val result = repository.getInventories()
            _uiState.update { state ->
                result.fold(
                    onSuccess = { inventories ->
                        state.copy(
                            inventories = inventories.sortedWith(NEWEST_FIRST),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            inventories = emptyList(),
                            isLoading = false,
                            error = it.toMessage(RString.inventories_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}

/** Newest created first; on the same day, by name ignoring case. */
private val NEWEST_FIRST: Comparator<Inventory> =
    compareByDescending<Inventory> { it.createdAt }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
