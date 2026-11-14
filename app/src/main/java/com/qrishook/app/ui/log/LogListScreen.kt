package com.qrishook.app.ui.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.ui.components.EmptyState
import com.qrishook.app.ui.components.LoadingState
import com.qrishook.app.ui.components.LogItem
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing

@Composable
fun LogListScreen(
    onBack: () -> Unit,
    onOpenLog: (Long) -> Unit,
    viewModel: LogListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val automationName by viewModel.automationName.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            QRISHookTopAppBar(
                title = automationName?.let { stringResource(R.string.log_title_automation, it) }
                    ?: stringResource(R.string.log_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                LogFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label()) },
                    )
                }
            }

            when {
                state.loading -> LoadingState()
                state.logs.isEmpty() && state.filter == LogFilter.ALL -> EmptyState(
                    icon = Icons.Outlined.ReceiptLong,
                    title = stringResource(R.string.log_empty_title),
                    description = stringResource(R.string.log_empty_description),
                    modifier = Modifier.padding(top = Spacing.xl),
                )
                state.logs.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.ReceiptLong,
                    title = stringResource(R.string.log_empty_filtered),
                    description = stringResource(R.string.log_empty_description),
                    modifier = Modifier.padding(top = Spacing.xl),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(items = state.logs, key = { it.id }) { log ->
                        LogItem(log = log, onClick = { onOpenLog(log.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFilter.label(): String = stringResource(
    when (this) {
        LogFilter.ALL -> R.string.filter_all
        LogFilter.SUCCESS -> R.string.status_success
        LogFilter.FAILED -> R.string.status_failed
        LogFilter.QUEUED -> R.string.status_queued
    },
)

@Preview(showBackground = true)
@Composable
private fun LogListPreview() {
    QRISHookTheme {
        Column(modifier = Modifier.padding(Spacing.md)) {
            LogItem(
                log = ExecutionLog(
                    id = 1,
                    automationId = 1,
                    automationName = "QRIS masuk",
                    triggerType = "Notifikasi",
                    status = LogStatus.SUCCESS,
                    httpCode = 200,
                ),
                onClick = {},
            )
        }
    }
}
