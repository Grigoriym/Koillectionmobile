package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_empty
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_section_children
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_section_wishes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** A tap on a wish does nothing yet: the wish screen comes in M9.6. */
@Composable
fun WishlistScreen(
    wishlistId: String,
    onWishlistClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistViewModel = koinViewModel { parametersOf(wishlistId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WishlistContent(uiState = uiState, onWishlistClick = onWishlistClick, onBack = onBack, modifier = modifier)
}

@Composable
private fun WishlistContent(
    uiState: WishlistUiState,
    onWishlistClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopBar(title = uiState.name, onBack = onBack) }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || (uiState.children.isEmpty() && uiState.wishes.isEmpty()) -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.wishlist_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                if (uiState.children.isNotEmpty()) {
                    item { SectionHeader(stringResource(RString.wishlist_section_children)) }
                    items(uiState.children, key = { "wishlist-${it.id}" }) { child ->
                        ListItem(
                            modifier = Modifier.clickable { onWishlistClick(child.id) },
                            headlineContent = { Text(child.name) },
                            leadingContent = { Thumbnail(imageUrl = child.imageUrl, name = child.name) }
                        )
                    }
                }
                if (uiState.wishes.isNotEmpty()) {
                    item { SectionHeader(stringResource(RString.wishlist_section_wishes)) }
                    items(uiState.wishes, key = { "wish-${it.id}" }) { wish -> WishRow(wish) }
                }
            }
        }
    }
}

@Composable
private fun WishRow(wish: Wish, modifier: Modifier = Modifier) {
    val price = listOfNotNull(wish.price, wish.currency).joinToString(" ")
    ListItem(
        modifier = modifier,
        headlineContent = { Text(wish.name) },
        supportingContent = if (wish.price != null) {
            { Text(price) }
        } else {
            null
        },
        leadingContent = { Thumbnail(imageUrl = wish.thumbnailUrl, name = wish.name) }
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(PADDING),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
