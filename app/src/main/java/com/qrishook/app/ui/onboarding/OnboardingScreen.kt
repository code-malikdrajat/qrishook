package com.qrishook.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.permission.PermissionState
import com.qrishook.app.ui.components.PermissionCard
import com.qrishook.app.ui.components.QRISHookButton
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing

@Composable
fun OnboardingScreen(
    onStart: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    Scaffold(topBar = { QRISHookTopAppBar(title = stringResource(R.string.app_name)) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            PermissionCard(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.permission_notification_access),
                description = stringResource(R.string.permission_notification_access_desc),
                granted = permissions.notificationAccess,
                onRequest = viewModel::openNotificationAccess,
            )
            PermissionCard(
                icon = Icons.Outlined.Notifications,
                label = stringResource(R.string.permission_post_notifications),
                description = stringResource(R.string.permission_post_notifications_desc),
                granted = permissions.notifications,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.openNotificationPermission()
                    }
                },
            )
            PermissionCard(
                icon = Icons.Default.BatterySaver,
                label = stringResource(R.string.permission_battery),
                description = stringResource(R.string.permission_battery_desc),
                granted = permissions.batteryUnrestricted,
                onRequest = viewModel::openBatteryOptimization,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            QRISHookButton(
                text = stringResource(R.string.onboarding_start),
                onClick = {
                    viewModel.finishOnboarding()
                    onStart()
                },
                enabled = permissions.allGranted,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    viewModel.finishOnboarding()
                    onStart()
                },
            ) {
                Text(text = stringResource(R.string.onboarding_skip))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    QRISHookTheme {
        OnboardingContentPreview(
            permissions = PermissionState(
                notificationAccess = true,
                notifications = false,
                batteryUnrestricted = false,
            ),
        )
    }
}

@Composable
private fun OnboardingContentPreview(permissions: PermissionState) {
    Column(modifier = Modifier.padding(Spacing.md)) {
        PermissionCard(
            icon = Icons.Outlined.Notifications,
            label = "Akses Notifikasi",
            granted = permissions.notificationAccess,
            onRequest = {},
        )
    }
}
