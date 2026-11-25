package com.grappim.koillectionmobile.feature.inventories.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grappim.kit.uikit.NativeText
import com.grappim.koillectionmobile.feature.inventories.domain.InventoriesRepository
import com.grappim.koillectionmobile.feature.inventories.domain.InventoryChanges
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.inventories_delete_error_unexpected
import com.grappim.koillectionmobile.strings.generated.resources.inventory_error_unexpected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reads one inventory. The app does not tick items: the checked marks are what the server stores.
 * A delete asks first, then tells the list to read again ([InventoryChanges]).
 *
 * [inventoryId] arrives through `parametersOf` at the call site. [InjectedParam] tells the Koin
 * compiler plugin not to look for a `String` definition in the graph.
 */
@KoinViewModel
class InventoryViewModel(
    @InjectedParam private val inventoryId: String,
    private val repository: InventoriesRepository,
    private val inventoryChanges: InventoryChanges
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryUiState(
            onRetry = ::retry,
            onToggle = ::toggle,
            onDeleteClick = ::askDelete,
            onDeleteConfirm = ::delete,
            onDeleteDismiss = ::dismissDelete,
            onDeleteErrorShown = ::clearDeleteError
        )
    )
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = NativeText.Empty) }
        load()
    }

    private fun toggle(collectionId: String) {
        _uiState.update { state ->
            val expanded = state.expandedIds
            state.copy(expandedIds = if (collectionId in expanded) expanded - collectionId else expanded + collectionId)
        }
    }

    private fun askDelete() {
        if (!_uiState.value.canDelete) return
        _uiState.update { it.copy(isDeleteConfirm = true, deleteError = NativeText.Empty) }
    }

    private fun dismissDelete() {
        _uiState.update { it.copy(isDeleteConfirm = false) }
    }

    private fun delete() {
        if (!_uiState.value.isDeleteConfirm) return
        _uiState.update { it.copy(isDeleteConfirm = false, isDeleting = true) }
        viewModelScope.launch {
            val result = repository.deleteInventory(inventoryId)
            _uiState.update { state ->
                state.copy(
                    isDeleting = false,
                    isDeleted = result.isSuccess,
                    deleteError = result.exceptionOrNull()?.toMessage(RString.inventories_delete_error_unexpected)
                        ?: NativeText.Empty
                )
            }
            result.onSuccess { inventoryChanges.notifyDeleted(inventoryId) }
        }
    }

    private fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = NativeText.Empty) }
    }

    private fun load() {
        viewModelScope.launch {
            val result = repository.getInventory(inventoryId)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { inventory ->
                        state.copy(
                            name = inventory.name,
                            collections = inventory.collections,
                            expandedIds = inventory.collections.singleOrNull()?.let { setOf(it.id) }.orEmpty(),
                            isLoading = false,
                            error = NativeText.Empty
                        )
                    },
                    onFailure = {
                        state.copy(
                            name = "",
                            collections = emptyList(),
                            expandedIds = emptySet(),
                            isLoading = false,
                            error = it.toMessage(RString.inventory_error_unexpected)
                        )
                    }
                )
            }
        }
    }
}
