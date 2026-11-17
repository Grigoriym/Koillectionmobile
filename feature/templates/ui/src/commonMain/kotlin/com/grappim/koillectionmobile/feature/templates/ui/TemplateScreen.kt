package com.grappim.koillectionmobile.feature.templates.ui

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
import com.grappim.koillectionmobile.strings.generated.resources.template_empty
import com.grappim.koillectionmobile.strings.generated.resources.template_field_choice_list
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The fields of template [templateId]. [name] is the title, as the template list showed it. */
@Composable
fun TemplateScreen(
    templateId: String,
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplateViewModel = koinViewModel { parametersOf(templateId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TemplateContent(uiState = uiState, name = name, onBack = onBack, modifier = modifier)
}

@Composable
private fun TemplateContent(uiState: TemplateUiState, name: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopBar(title = name, onBack = onBack) }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when {
            uiState.isLoading -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.error.isNotEmpty() || uiState.fields.isEmpty() -> EmptyOrError(
                modifier = contentModifier,
                error = uiState.error,
                emptyText = stringResource(RString.template_empty),
                onRetry = uiState.onRetry
            )

            else -> LazyColumn(modifier = contentModifier) {
                items(uiState.fields, key = { it.id }) { field ->
                    ListItem(
                        headlineContent = { Text(field.name) },
                        supportingContent = {
                            val choiceList = field.choiceListName
                            Text(
                                if (choiceList == null) {
                                    field.type
                                } else {
                                    stringResource(RString.template_field_choice_list, field.type, choiceList)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
