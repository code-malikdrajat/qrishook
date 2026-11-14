package com.qrishook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qrishook.app.R
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.ui.theme.Spacing
import com.qrishook.app.util.formatRelativeTime

@Composable
fun AutomationCard(
    automation: Automation,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trigger = automation.trigger
    val subtitle = when (trigger) {
        is Trigger.NotificationTrigger -> stringResource(
            R.string.automation_subtitle_notification,
            trigger.sourceApps.takeIf { it.isNotEmpty() }?.joinToString(", ")
                ?: stringResource(R.string.label_all_apps),
        )
        is Trigger.IntervalTrigger -> stringResource(
            R.string.automation_subtitle_interval,
            trigger.intervalMinutes,
        )
    }
    val toggleDescription = stringResource(R.string.cd_toggle_automation, automation.name)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (automation.enabled) 1f else 0.6f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (trigger) {
                        is Trigger.NotificationTrigger -> Icons.Outlined.Notifications
                        is Trigger.IntervalTrigger -> Icons.Outlined.Schedule
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = automation.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = automation.lastRunAt?.let {
                        stringResource(R.string.automation_last_run, formatRelativeTime(it))
                    } ?: stringResource(R.string.automation_never_run),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = automation.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AutomationCardPreview() {
    MaterialTheme {
        AutomationCard(
            automation = Automation(
                id = 1,
                name = "QRIS masuk BCA",
                trigger = Trigger.NotificationTrigger(keywords = listOf("QRIS")),
                action = Action(url = "https://example.com/hook"),
                lastRunAt = System.currentTimeMillis() - 300_000,
            ),
            onToggle = {},
            onClick = {},
        )
    }
}
