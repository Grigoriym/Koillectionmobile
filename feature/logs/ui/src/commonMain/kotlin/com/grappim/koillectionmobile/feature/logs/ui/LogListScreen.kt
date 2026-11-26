package com.grappim.koillectionmobile.feature.logs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.NativeText
import com.grappim.kit.uikit.asString
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.NavigationIconConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarConfig
import com.grappim.koillectionmobile.core.ui.EmptyOrError
import com.grappim.koillectionmobile.feature.logs.domain.LogAction
import com.grappim.koillectionmobile.feature.logs.domain.LogEntry
import com.grappim.koillectionmobile.feature.logs.domain.LogObjectType
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.logs_action_create
import com.grappim.koillectionmobile.strings.generated.resources.logs_action_delete
import com.grappim.koillectionmobile.strings.generated.resources.logs_action_unknown
import com.grappim.koillectionmobile.strings.generated.resources.logs_empty
import com.grappim.koillectionmobile.strings.generated.resources.logs_title
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_album
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_choice_list
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_collection
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_inventory
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_item
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_photo
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_tag
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_tag_category
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_template
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_unknown
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_wish
import com.grappim.koillectionmobile.strings.generated.resources.logs_type_wishlist
import com.grappim.koillectionmobile.strings.generated.resources.retry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The root of the Logs section: view only (plan §5 decision 16). Each row shows the action with
 * the object type, the object label, and the date with the time. The next page loads when the
 * user scrolls near the end.
 */
@Composable
fun LogListScreen(modifier: Modifier = Modifier, viewModel: LogListViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarController = LocalTopBarConfig.current
    LaunchedEffect(Unit) {
        topBarController.update(
            TopBarConfig(
                title = NativeText.Resource(RString.logs_title),
                navigationIcon = NavigationIconConfig.Menu
            )
        )
    }
    val contentModifier = modifier.fillMaxSize()
    when {
        uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.error.isNotEmpty() || uiState.logs.isEmpty() -> EmptyOrError(
            modifier = contentModifier,
            error = uiState.error,
            emptyText = stringResource(RString.logs_empty),
            onRetry = uiState.onRetry
        )

        else -> LogList(uiState = uiState, modifier = contentModifier)
    }
}

@Composable
private fun LogList(uiState: LogListUiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val isNearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    // Keyed on the size too: a page that adds few rows can leave the list near the end.
    LaunchedEffect(isNearEnd, uiState.logs.size) {
        if (isNearEnd) uiState.onLoadMore()
    }
    LazyColumn(modifier = modifier, state = listState) {
        items(uiState.logs, key = { it.id }) { log -> LogRow(log) }
        item(key = MORE_KEY) { MoreFooter(uiState) }
    }
}

@Composable
private fun LogRow(log: LogEntry) {
    val type = stringResource(log.objectType.label)
    ListItem(
        overlineContent = { Text(stringResource(log.action.label, type)) },
        headlineContent = { Text(log.objectLabel) },
        supportingContent = { Text(LOGGED_AT_FORMAT.format(log.loggedAt)) }
    )
}

@Composable
private fun MoreFooter(uiState: LogListUiState) {
    when {
        uiState.isLoadingMore -> Box(
            modifier = Modifier.fillMaxWidth().padding(PADDING),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        uiState.moreError.isNotEmpty() -> Column(
            modifier = Modifier.fillMaxWidth().padding(PADDING),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.moreError.asString(),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = uiState.onRetryMore) { Text(stringResource(RString.retry)) }
        }
    }
}

/** A format string with the object type as `%1$s`. */
private val LogAction.label: StringResource
    get() = when (this) {
        LogAction.Create -> RString.logs_action_create
        LogAction.Delete -> RString.logs_action_delete
        LogAction.Unknown -> RString.logs_action_unknown
    }

private val LogObjectType.label: StringResource
    get() = when (this) {
        LogObjectType.Collection -> RString.logs_type_collection
        LogObjectType.Item -> RString.logs_type_item
        LogObjectType.Template -> RString.logs_type_template
        LogObjectType.ChoiceList -> RString.logs_type_choice_list
        LogObjectType.Inventory -> RString.logs_type_inventory
        LogObjectType.Tag -> RString.logs_type_tag
        LogObjectType.TagCategory -> RString.logs_type_tag_category
        LogObjectType.Album -> RString.logs_type_album
        LogObjectType.Photo -> RString.logs_type_photo
        LogObjectType.Wishlist -> RString.logs_type_wishlist
        LogObjectType.Wish -> RString.logs_type_wish
        LogObjectType.Unknown -> RString.logs_type_unknown
    }

/** `yyyy-MM-dd HH:mm`, as the server wrote it (`LogMapper` does no time zone conversion). */
private val LOGGED_AT_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}

/** The next page loads when the last visible row is this close to the end. */
private const val LOAD_MORE_THRESHOLD = 10
private const val MORE_KEY = "more"
private val PADDING = 16.dp
