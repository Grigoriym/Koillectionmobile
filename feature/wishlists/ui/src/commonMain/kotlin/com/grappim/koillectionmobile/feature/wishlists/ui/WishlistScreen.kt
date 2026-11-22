package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.feature.wishlists.domain.Wish
import com.grappim.koillectionmobile.strings.RPlurals
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_cancel
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_confirm
import com.grappim.koillectionmobile.strings.generated.resources.collection_delete_failed_ok
import com.grappim.koillectionmobile.strings.generated.resources.wish_new
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_children
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_failed_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_text
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_title
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_delete_wishes
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_edit
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_empty
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_more
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_new
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_section_children
import com.grappim.koillectionmobile.strings.generated.resources.wishlist_section_wishes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [onClose] runs once the server deleted the wishlist, to take the screen off the back stack. */
@Composable
fun WishlistScreen(
    wishlistId: String,
    onWishlistClick: (String) -> Unit,
    onWishClick: (String) -> Unit,
    onNewWishlistClick: () -> Unit,
    onNewWishClick: () -> Unit,
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistViewModel = koinViewModel { parametersOf(wishlistId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) currentOnClose()
    }
    WishlistContent(
        uiState = uiState,
        onWishlistClick = onWishlistClick,
        onWishClick = onWishClick,
        onNewWishlistClick = onNewWishlistClick,
        onNewWishClick = onNewWishClick,
        onEditClick = onEditClick,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun WishlistContent(
    uiState: WishlistUiState,
    onWishlistClick: (String) -> Unit,
    onWishClick: (String) -> Unit,
    onNewWishlistClick: () -> Unit,
    onNewWishClick: () -> Unit,
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BackTopBar(title = uiState.name, onBack = onBack) {
                if (uiState.isCountingDelete || uiState.isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.padding(PADDING / 2).size(PROGRESS_SIZE))
                } else if (uiState.canDelete) {
                    IconButton(onClick = uiState.onDeleteClick) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(RString.wishlist_delete)
                        )
                    }
                }
                MoreMenu(
                    onNewWishlistClick = onNewWishlistClick,
                    onNewWishClick = onNewWishClick,
                    onEditClick = onEditClick
                )
            }
        }
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
                    items(uiState.wishes, key = { "wish-${it.id}" }) { wish ->
                        WishRow(wish = wish, modifier = Modifier.clickable { onWishClick(wish.id) })
                    }
                }
            }
        }
    }
    DeleteDialogs(uiState)
}

@Composable
private fun DeleteDialogs(uiState: WishlistUiState) {
    uiState.deleteSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = uiState.onDeleteDismiss,
            title = { Text(stringResource(RString.wishlist_delete_title)) },
            text = {
                val children =
                    pluralStringResource(RPlurals.wishlist_delete_children, summary.childCount, summary.childCount)
                val wishes = pluralStringResource(RPlurals.wishlist_delete_wishes, summary.wishCount, summary.wishCount)
                Text(stringResource(RString.wishlist_delete_text, uiState.name, children, wishes))
            },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteConfirm) {
                    Text(stringResource(RString.collection_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = uiState.onDeleteDismiss) {
                    Text(stringResource(RString.collection_delete_cancel))
                }
            }
        )
    }
    if (uiState.deleteError.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = uiState.onDeleteErrorShown,
            title = { Text(stringResource(RString.wishlist_delete_failed_title)) },
            text = { Text(uiState.deleteError.asString()) },
            confirmButton = {
                TextButton(onClick = uiState.onDeleteErrorShown) {
                    Text(stringResource(RString.collection_delete_failed_ok))
                }
            }
        )
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

/** The top bar's overflow menu. */
@Composable
private fun MoreMenu(onNewWishlistClick: () -> Unit, onNewWishClick: () -> Unit, onEditClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(RString.wishlist_more))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(RString.wishlist_new)) },
                onClick = {
                    isExpanded = false
                    onNewWishlistClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(RString.wish_new)) },
                onClick = {
                    isExpanded = false
                    onNewWishClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(RString.wishlist_edit)) },
                onClick = {
                    isExpanded = false
                    onEditClick()
                }
            )
        }
    }
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

private val PROGRESS_SIZE = 24.dp
