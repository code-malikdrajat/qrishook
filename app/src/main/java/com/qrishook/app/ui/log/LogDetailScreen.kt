package com.qrishook.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.ui.components.ErrorBanner
import com.qrishook.app.ui.components.KeyValueRow
import com.qrishook.app.ui.components.LoadingState
import com.qrishook.app.ui.components.QRISHookCard
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.components.SectionHeader
import com.qrishook.app.ui.components.StatusChip
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing
import com.qrishook.app.util.formatDateTime

@Composable
fun LogDetailScreen(
    onBack: () -> Unit,
    viewModel: LogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            QRISHookTopAppBar(
                title = stringResource(R.string.log_detail_title),
                onBack = onBack,
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
                LogDetailUiState.Loading -> LoadingState()
                LogDetailUiState.NotFound -> ErrorBanner(
                    message = stringResource(R.string.log_not_found),
                )
                is LogDetailUiState.Content -> {
                    LogHeaderCard(log = current.log)

                    QRISHookCard {
                        SectionHeader(stringResource(R.string.log_info_section))
                        KeyValueRow(stringResource(R.string.log_trigger), current.log.triggerType)
                        KeyValueRow(
                            stringResource(R.string.log_http),
                            current.log.httpCode?.toString()
                                ?: stringResource(R.string.wizard_summary_none),
                        )
                        KeyValueRow(
                            stringResource(R.string.log_duration),
                            current.log.durationMs?.let { "$it ms" }
                                ?: stringResource(R.string.wizard_summary_none),
                        )
                        current.log.errorMessage?.let {
                            KeyValueRow(stringResource(R.string.log_error), it)
                        }
                    }

                    SectionHeader(stringResource(R.string.log_payload))
                    QRISHookCard {
                        SelectionContainer {
                            Text(
                                text = current.log.payloadSent.ifBlank {
                                    stringResource(R.string.wizard_summary_none)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(current.log.payloadSent)) },
                        ) {
                            Text(stringResource(R.string.action_copy))
                        }
                    }

                    SectionHeader(stringResource(R.string.log_response))
                    QRISHookCard {
                        SelectionContainer {
                            Text(
                                text = current.log.responseSnippet?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.wizard_summary_none),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(current.log.responseSnippet.orEmpty()))
                            },
                        ) {
                            Text(stringResource(R.string.action_copy))
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::resend,
                        enabled = !current.resending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogHeaderCard(log: ExecutionLog) {
    val container = when (log.status) {
        LogStatus.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
        LogStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        LogStatus.QUEUED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (log.status) {
        LogStatus.SUCCESS -> MaterialTheme.colorScheme.onTertiaryContainer
        LogStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        LogStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = container,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = when (log.status) {
                        LogStatus.SUCCESS -> Icons.Default.CheckCircle
                        LogStatus.FAILED -> Icons.Default.Error
                        LogStatus.QUEUED -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                StatusChip(status = log.status)
            }
            Text(
                text = log.automationName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Text(
                text = formatDateTime(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogHeaderPreview() {
    QRISHookTheme {
        LogHeaderCard(
            log = ExecutionLog(
                id = 1,
                automationId = 1,
                automationName = "QRIS masuk BCA",
                triggerType = "Notifikasi",
                status = LogStatus.SUCCESS,
                httpCode = 200,
            ),
        )
    }
}
