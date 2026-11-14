package com.qrishook.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.qrishook.app.R
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.ui.theme.Spacing

@Composable
fun StatusChip(status: LogStatus, modifier: Modifier = Modifier) {
    val (label, icon, container, content) = when (status) {
        LogStatus.SUCCESS -> StatusStyle(
            R.string.status_success,
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        LogStatus.FAILED -> StatusStyle(
            R.string.status_failed,
            Icons.Default.Error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        LogStatus.QUEUED -> StatusStyle(
            R.string.status_queued,
            Icons.Default.Schedule,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    AssistChip(
        onClick = {},
        label = { Text(stringResource(label)) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(Spacing.xs))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = container,
            labelColor = content,
            leadingIconContentColor = content,
        ),
        shape = MaterialTheme.shapes.small,
    )
}

private data class StatusStyle(
    val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
)

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    MaterialTheme {
        StatusChip(status = LogStatus.SUCCESS)
    }
}
