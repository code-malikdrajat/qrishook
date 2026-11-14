package com.qrishook.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qrishook.app.R
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.ui.theme.Spacing
import com.qrishook.app.util.formatDateTime

@Composable
fun LogItem(
    log: ExecutionLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusLabel = stringResource(
        when (log.status) {
            LogStatus.SUCCESS -> R.string.status_success
            LogStatus.FAILED -> R.string.status_failed
            LogStatus.QUEUED -> R.string.status_queued
        },
    )
    val tint = when (log.status) {
        LogStatus.SUCCESS -> MaterialTheme.colorScheme.tertiary
        LogStatus.FAILED -> MaterialTheme.colorScheme.error
        LogStatus.QUEUED -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (log.status) {
                LogStatus.SUCCESS -> Icons.Default.CheckCircle
                LogStatus.FAILED -> Icons.Default.Error
                LogStatus.QUEUED -> Icons.Default.Schedule
            },
            contentDescription = statusLabel,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.automationName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val code = log.httpCode?.let { " · $it" }.orEmpty()
            Text(
                text = "${log.triggerType} · $statusLabel$code",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatDateTime(log.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LogItemPreview() {
    MaterialTheme {
        LogItem(
            log = ExecutionLog(
                id = 1,
                automationId = 1,
                automationName = "QRIS masuk BCA",
                triggerType = "Notifikasi",
                status = LogStatus.SUCCESS,
                httpCode = 200,
            ),
            onClick = {},
        )
    }
}
