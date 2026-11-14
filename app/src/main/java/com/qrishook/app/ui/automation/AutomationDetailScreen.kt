package com.qrishook.app.ui.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.ui.components.ErrorBanner
import com.qrishook.app.ui.components.KeyValueRow
import com.qrishook.app.ui.components.LoadingState
import com.qrishook.app.ui.components.QRISHookCard
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.components.SectionHeader
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing
import com.qrishook.app.util.formatRelativeTime

@Composable
fun AutomationDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenLogs: (Long) -> Unit,
    viewModel: AutomationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val content = state as? DetailUiState.Content

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    Scaffold(
        topBar = {
            QRISHookTopAppBar(
                title = content?.automation?.name ?: stringResource(R.string.detail_title),
                onBack = onBack,
                actions = {
                    if (content != null) {
                        IconButton(onClick = { onEdit(content.automation.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (val current = state) {
                DetailUiState.Loading -> LoadingState()
                DetailUiState.NotFound -> ErrorBanner(
                    message = stringResource(R.string.detail_not_found),
                )
                is DetailUiState.Content -> {
                    StatusCard(
                        enabled = current.automation.enabled,
                        onToggle = viewModel::setEnabled,
                    )
                    TriggerCard(automation = current.automation)
                    ActionCard(
                        method = current.automation.action.method.name,
                        url = current.webhookUrl,
                    )
                    StatsCard(state = current)
                    TextButton(onClick = { onOpenLogs(current.automation.id) }) {
                        Text(stringResource(R.string.detail_view_logs))
                    }
                }
            }
        }
    }

    if (showDeleteDialog && content != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = {
                Text(stringResource(R.string.detail_delete_message, content.automation.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    QRISHookCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.detail_status),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(if (enabled) R.string.detail_enabled else R.string.detail_disabled),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun TriggerCard(automation: Automation) {
    val trigger = automation.trigger
    QRISHookCard {
        SectionHeader(stringResource(R.string.detail_trigger))
        when (trigger) {
            is Trigger.NotificationTrigger -> {
                KeyValueRow(
                    label = stringResource(R.string.detail_apps),
                    value = trigger.sourceApps.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: stringResource(R.string.label_all_apps),
                )
                KeyValueRow(
                    label = stringResource(R.string.detail_keywords),
                    value = trigger.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: stringResource(R.string.wizard_summary_none),
                )
            }
            is Trigger.IntervalTrigger -> {
                KeyValueRow(
                    label = stringResource(R.string.trigger_interval),
                    value = stringResource(R.string.automation_subtitle_interval, trigger.intervalMinutes),
                )
            }
        }
    }
}

@Composable
private fun ActionCard(method: String, url: String) {
    QRISHookCard {
        SectionHeader(stringResource(R.string.detail_action))
        KeyValueRow(label = stringResource(R.string.detail_method), value = method)
        KeyValueRow(label = stringResource(R.string.detail_url), value = url)
    }
}

@Composable
private fun StatsCard(state: DetailUiState.Content) {
    QRISHookCard {
        SectionHeader(stringResource(R.string.detail_stats))
        KeyValueRow(
            label = stringResource(R.string.detail_last_run),
            value = state.automation.lastRunAt?.let { formatRelativeTime(it) }
                ?: stringResource(R.string.automation_never_run),
        )
        KeyValueRow(
            label = stringResource(R.string.detail_total),
            value = state.stats.total.toString(),
        )
        KeyValueRow(label = stringResource(R.string.status_success), value = state.stats.success.toString())
        KeyValueRow(label = stringResource(R.string.status_failed), value = state.stats.failed.toString())
    }
}

@Preview(showBackground = true)
@Composable
private fun AutomationDetailPreview() {
    QRISHookTheme {
        Column(modifier = Modifier.padding(Spacing.md)) {
            TriggerCard(
                automation = Automation(
                    id = 1,
                    name = "QRIS masuk",
                    trigger = Trigger.NotificationTrigger(keywords = listOf("QRIS")),
                    action = Action(url = "https://example.com/hook"),
                ),
            )
        }
    }
}
