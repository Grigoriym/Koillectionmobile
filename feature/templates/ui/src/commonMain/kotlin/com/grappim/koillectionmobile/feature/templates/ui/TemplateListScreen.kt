package com.grappim.koillectionmobile.feature.templates.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.koillectionmobile.feature.templates.domain.Template
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.templates_empty
import com.grappim.koillectionmobile.strings.generated.resources.templates_new
import com.grappim.koillectionmobile.strings.generated.resources.templates_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TemplateListScreen(
    onTemplateClick: (Template) -> Unit,
    onNewTemplateClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplateListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TemplateListContent(
        uiState = uiState,
        onTemplateClick = onTemplateClick,
        onNewTemplateClick = onNewTemplateClick,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun TemplateListContent(
    uiState: TemplateListUiState,
    onTemplateClick: (Template) -> Unit,
    onNewTemplateClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopBar(title = stringResource(RString.templates_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTemplateClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(RString.templates_new))
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

            uiState.error.isNotEmpty() || uiState.templates.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.templates_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.templates, key = { it.id }) { template ->
                    ListItem(
                        modifier = Modifier.clickable { onTemplateClick(template) },
                        headlineContent = { Text(template.name) }
                    )
                }
            }
        }
    }
}
