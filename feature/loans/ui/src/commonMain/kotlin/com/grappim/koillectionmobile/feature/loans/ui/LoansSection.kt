package com.grappim.koillectionmobile.feature.loans.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grappim.kit.uikit.asString
import com.grappim.koillectionmobile.feature.loans.domain.Loan
import com.grappim.koillectionmobile.strings.RString
import com.grappim.koillectionmobile.strings.generated.resources.loans_empty
import com.grappim.koillectionmobile.strings.generated.resources.loans_lent_at
import com.grappim.koillectionmobile.strings.generated.resources.loans_new
import com.grappim.koillectionmobile.strings.generated.resources.loans_not_returned
import com.grappim.koillectionmobile.strings.generated.resources.loans_retry
import com.grappim.koillectionmobile.strings.generated.resources.loans_returned_at
import com.grappim.koillectionmobile.strings.generated.resources.loans_section
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The "Loans" section of the item screen. It loads on its own: a slow or failed read shows here
 * only. The dates show as `yyyy-MM-dd`. [onNewLoan] opens the new-loan form; [onLoanClick] opens
 * the edit form of a loan, by id.
 */
@Composable
fun LoansSection(
    itemId: String,
    onNewLoan: () -> Unit,
    onLoanClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoansViewModel = koinViewModel { parametersOf(itemId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoansContent(uiState = uiState, onNewLoan = onNewLoan, onLoanClick = onLoanClick, modifier = modifier)
}

@Composable
private fun LoansContent(
    uiState: LoansUiState,
    onNewLoan: () -> Unit,
    onLoanClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SPACING)) {
        Text(
            text = stringResource(RString.loans_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE))

            uiState.error.isNotEmpty() -> {
                Text(text = uiState.error.asString(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = uiState.onRetry) { Text(stringResource(RString.loans_retry)) }
            }

            else -> {
                if (uiState.loans.isEmpty()) {
                    Text(
                        text = stringResource(RString.loans_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.loans.forEach { loan -> LoanRow(loan, onClick = { onLoanClick(loan.id) }) }
                TextButton(onClick = onNewLoan) { Text(stringResource(RString.loans_new)) }
            }
        }
    }
}

@Composable
private fun LoanRow(loan: Loan, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(text = loan.lentTo, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(RString.loans_lent_at, loan.lentAt.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = loan.returnedAt?.let { stringResource(RString.loans_returned_at, it.toString()) }
                ?: stringResource(RString.loans_not_returned),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SPACING = 8.dp
private val PROGRESS_SIZE = 24.dp
