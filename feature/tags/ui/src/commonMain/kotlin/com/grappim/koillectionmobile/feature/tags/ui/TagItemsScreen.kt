package com.grappim.koillectionmobile.feature.tags.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.tag_items_empty
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TagItemsScreen(
    tagId: String,
    label: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagItemsViewModel = koinViewModel { parametersOf(tagId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarController = LocalTopBarConfig.current
    LaunchedEffect(label) {
        topBarController.update(
            TopBarConfig(title = NativeText.Simple(label), navigationIcon = NavigationIconConfig.Back())
        )
    }
    TagItemsContent(uiState = uiState, onItemClick = onItemClick, modifier = modifier)
}

@Composable
private fun TagItemsContent(uiState: TagItemsUiState, onItemClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.items.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.tag_items_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.items, key = { it.itemId }) { item ->
                    ListItem(
                        modifier = Modifier.clickable { onItemClick(item.itemId) },
                        headlineContent = { Text(item.name) }
                    )
                }
            }
        }
    }
}
