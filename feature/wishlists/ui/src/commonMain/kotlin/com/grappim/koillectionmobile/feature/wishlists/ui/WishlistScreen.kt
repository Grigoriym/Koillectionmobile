package com.grappim.koillectionmobile.feature.wishlists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarActionMenu
import com.grappim.kit.uikit.widgets.topbar.TopBarActionVectorButton
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarMenuItem
import com.grappim.koillectionmobile.core.ui.EmptyOrError
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
import kotlinx.collections.immutable.persistentListOf
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
    modifier: Modifier = Modifier
) {
    val topBarController = LocalTopBarConfig.current
    val deleteLabel = stringResource(RString.wishlist_delete)
    val moreLabel = stringResource(RString.wishlist_more)
    val currentOnDeleteClick by rememberUpdatedState(uiState.onDeleteClick)
    val currentOnNewWishlistClick by rememberUpdatedState(onNewWishlistClick)
    val currentOnNewWishClick by rememberUpdatedState(onNewWishClick)
    val currentOnEditClick by rememberUpdatedState(onEditClick)
    // The kit bar has no progress slot: while the delete runs, Delete stays, disabled, and the
    // progress shows under the bar.
    val isBusy = uiState.isCountingDelete || uiState.isDeleting
    val hasDelete = uiState.canDelete || isBusy
    LaunchedEffect(uiState.name, hasDelete, isBusy, deleteLabel, moreLabel) {
        val menu = TopBarActionMenu(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = moreLabel,
            items = persistentListOf(
                TopBarMenuItem(
                    text = NativeText.Resource(RString.wishlist_new),
                    onClick = { currentOnNewWishlistClick() }
                ),
                TopBarMenuItem(
                    text = NativeText.Resource(RString.wish_new),
                    onClick = { currentOnNewWishClick() }
                ),
                TopBarMenuItem(
                    text = NativeText.Resource(RString.wishlist_edit),
                    onClick = { currentOnEditClick() }
                )
            )
        )
        topBarController.update(
            TopBarConfig(
                title = NativeText.Simple(uiState.name),
                navigationIcon = NavigationIconConfig.Back(),
                actions = if (hasDelete) {
                    persistentListOf(
                        TopBarActionVectorButton(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = deleteLabel,
                            enabled = !isBusy,
                            onClick = { currentOnDeleteClick() }
                        ),
                        menu
                    )
                } else {
                    persistentListOf(menu)
                }
            )
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(PADDING),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
