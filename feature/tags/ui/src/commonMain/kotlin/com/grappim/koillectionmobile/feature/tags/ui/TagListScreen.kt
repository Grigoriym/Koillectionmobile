package com.grappim.koillectionmobile.feature.tags.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.feature.tags.domain.Tag
import com.grappim.koillectionmobile.feature.tags.domain.TagGroup
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.tags_empty
import com.grappim.koillectionmobile.strings.generated.resources.tags_no_category
import com.grappim.koillectionmobile.strings.generated.resources.tags_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TagListScreen(
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarController = LocalTopBarConfig.current
    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(title = NativeText.Resource(RString.tags_title), navigationIcon = NavigationIconConfig.Menu)
        )
    }
    TagListContent(uiState = uiState, onTagClick = onTagClick, modifier = modifier)
}

@Composable
private fun TagListContent(uiState: TagListUiState, onTagClick: (Tag) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.groups.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.tags_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                // Two categories may share a label, so a header is keyed by its position.
                uiState.groups.forEachIndexed { index, group ->
                    item(key = "group:$index") { GroupHeader(group) }
                    items(group.tags, key = { it.id }) { tag ->
                        ListItem(
                            modifier = Modifier.clickable { onTagClick(tag) },
                            headlineContent = { Text(tag.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: TagGroup) {
    Row(
        modifier = Modifier.padding(start = PADDING, end = PADDING, top = PADDING, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        group.color?.toColor()?.let { color ->
            Box(
                modifier = Modifier
                    .size(SWATCH)
                    .background(color = color, shape = CircleShape)
            )
        }
        Text(
            text = group.label ?: stringResource(RString.tags_no_category),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** The server's six-digit hex colour (`F3E5F5`), opaque; null for any other form. */
internal fun String.toColor(): Color? = removePrefix("#")
    .takeIf { it.length == HEX_RGB_LENGTH }
    ?.toLongOrNull(radix = 16)
    ?.let { Color(OPAQUE or it) }

private const val HEX_RGB_LENGTH = 6
private const val OPAQUE = 0xFF000000
private val SWATCH = 12.dp
