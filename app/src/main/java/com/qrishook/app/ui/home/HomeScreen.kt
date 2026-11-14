package com.qrishook.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.permission.PermissionState
import com.qrishook.app.ui.components.AutomationCard
import com.qrishook.app.ui.components.EmptyState
import com.qrishook.app.ui.components.ErrorBanner
import com.qrishook.app.ui.components.LoadingState
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing

@Composable
fun HomeScreen(
    onCreate: () -> Unit,
    onOpenAutomation: (Long) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    HomeContent(
        uiState = uiState,
        permissions = permissions,
        snackbarHostState = snackbarHostState,
        onCreate = onCreate,
        onOpenAutomation = onOpenAutomation,
        onOpenLogs = onOpenLogs,
        onOpenSettings = onOpenSettings,
        onOpenPermissionSettings = viewModel::openPermissionSettings,
        onToggle = viewModel::setEnabled,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    permissions: PermissionState,
    snackbarHostState: SnackbarHostState,
    onCreate: () -> Unit,
    onOpenAutomation: (Long) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onToggle: (Automation, Boolean) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            QRISHookTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onOpenLogs) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.cd_logs),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_fab)) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                HomeUiState.Loading -> LoadingState()
                is HomeUiState.Content -> Column(modifier = Modifier.fillMaxSize()) {
                    if (!permissions.allGranted) {
                        ErrorBanner(
                            message = stringResource(R.string.home_permission_banner),
                            actionLabel = stringResource(R.string.action_open),
                            onAction = onOpenPermissionSettings,
                            modifier = Modifier.padding(Spacing.md),
                        )
                    }
                    if (state.automations.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Inbox,
                            title = stringResource(R.string.home_empty_title),
                            description = stringResource(R.string.home_empty_description),
                            actionLabel = stringResource(R.string.home_fab),
                            onAction = onCreate,
                            modifier = Modifier.padding(top = Spacing.xl),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(items = state.automations, key = { it.id }) { automation ->
                                AutomationCard(
                                    automation = automation,
                                    onToggle = { enabled -> onToggle(automation, enabled) },
                                    onClick = { onOpenAutomation(automation.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    val sample = Automation(
        id = 1,
        name = "QRIS masuk BCA",
        trigger = Trigger.NotificationTrigger(sourceApps = listOf("id.co.bca")),
        action = Action(url = "https://example.com/hook"),
    )
    QRISHookTheme {
        HomeContent(
            uiState = HomeUiState.Content(listOf(sample)),
            permissions = PermissionState(true, true, true),
            snackbarHostState = remember { SnackbarHostState() },
            onCreate = {},
            onOpenAutomation = {},
            onOpenLogs = {},
            onOpenSettings = {},
            onOpenPermissionSettings = {},
            onToggle = { _, _ -> },
        )
    }
}
