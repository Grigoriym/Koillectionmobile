package com.grappim.koillectionmobile.feature.wishlists.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_empty
import com.grappim.koillectionmobile.strings.generated.resources.wishlists_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WishlistListScreen(
    onWishlistClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WishlistListContent(uiState = uiState, onWishlistClick = onWishlistClick, onBack = onBack, modifier = modifier)
}

@Composable
private fun WishlistListContent(
    uiState: WishlistListUiState,
    onWishlistClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopBar(title = stringResource(RString.wishlists_title), onBack = onBack) }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.wishlists.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.wishlists_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.wishlists, key = { it.id }) { wishlist ->
                    ListItem(
                        modifier = Modifier.clickable { onWishlistClick(wishlist.id) },
                        headlineContent = { Text(wishlist.name) },
                        leadingContent = { Thumbnail(imageUrl = wishlist.imageUrl, name = wishlist.name) }
                    )
                }
            }
        }
    }
}
