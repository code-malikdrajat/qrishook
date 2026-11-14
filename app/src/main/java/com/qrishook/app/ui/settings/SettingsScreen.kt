package com.qrishook.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.permission.PermissionState
import com.qrishook.app.ui.components.QRISHookCard
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.components.SectionHeader
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            QRISHookTopAppBar(
                title = stringResource(R.string.settings_title),
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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionHeader(stringResource(R.string.settings_permissions))
            PermissionRow(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.permission_notification_access),
                granted = state.permissions.notificationAccess,
                onClick = viewModel::openNotificationAccess,
            )
            PermissionRow(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.permission_post_notifications),
                granted = state.permissions.notifications,
                onClick = viewModel::openNotificationPermission,
            )
            PermissionRow(
                icon = Icons.Default.BatterySaver,
                label = stringResource(R.string.permission_battery),
                granted = state.permissions.batteryUnrestricted,
                onClick = viewModel::openBatteryOptimization,
            )

            SectionHeader(stringResource(R.string.settings_troubleshooting))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_service_check)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (state.serviceRunning) {
                                R.string.settings_service_running
                            } else {
                                R.string.settings_service_stopped
                            },
                        ),
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (state.serviceRunning) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_oem_guide)) },
                trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = viewModel::openOemGuide),
            )

            SectionHeader(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(state.version) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_source_code)) },
                trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = viewModel::openSourceCode),
            )

            QRISHookCard {
                Text(
                    text = stringResource(R.string.settings_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        headlineContent = { Text(label) },
        trailingContent = {
            if (granted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.cd_permission_granted),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                Text(
                    text = stringResource(R.string.action_open),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    QRISHookTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            PermissionRow(
                icon = Icons.Outlined.Notifications,
                label = "Akses Notifikasi",
                granted = true,
                onClick = {},
            )
            QRISHookCard {
                Text(
                    text = "Aplikasi ini tidak berafiliasi dengan bank atau penyedia e-wallet manapun.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
