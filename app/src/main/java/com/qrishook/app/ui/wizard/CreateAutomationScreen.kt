package com.qrishook.app.ui.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrishook.app.R
import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.domain.execution.PayloadTemplates
import com.qrishook.app.domain.webhook.TemplateEngine
import com.qrishook.app.ui.components.HeaderRow
import com.qrishook.app.ui.components.KeyValueRow
import com.qrishook.app.ui.components.PlaceholderChip
import com.qrishook.app.ui.components.QRISHookButton
import com.qrishook.app.ui.components.QRISHookCard
import com.qrishook.app.ui.components.QRISHookTextField
import com.qrishook.app.ui.components.QRISHookTopAppBar
import com.qrishook.app.ui.components.SectionHeader
import com.qrishook.app.ui.components.TriggerKind
import com.qrishook.app.ui.components.TriggerOptionCard
import com.qrishook.app.ui.theme.QRISHookTheme
import com.qrishook.app.ui.theme.Spacing

private val PLACEHOLDERS = listOf("{{title}}", "{{text}}", "{{app}}", "{{timestamp}}", "{{device}}")
private val QUICK_INTERVALS = listOf(15, 30, 60, 360, 1440)

@Composable
fun CreateAutomationScreen(
    onBackToHome: () -> Unit,
    viewModel: WizardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showUntestedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBackToHome()
    }

    val stepTitle = stringResource(
        when (state.step) {
            0 -> R.string.wizard_step_name
            1 -> R.string.wizard_step_trigger
            2 -> R.string.wizard_step_config
            3 -> R.string.wizard_step_webhook
            else -> R.string.wizard_step_test
        },
    )

    Scaffold(
        topBar = {
            QRISHookTopAppBar(
                title = stringResource(
                    if (state.isEdit) R.string.wizard_title_edit else R.string.wizard_title,
                ),
                onBack = { if (state.step > 0) viewModel.back() else onBackToHome() },
                actions = {
                    TextButton(onClick = onBackToHome) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        },
        bottomBar = {
            WizardBottomBar(
                step = state.step,
                canNext = state.canGoNext,
                saving = state.saving,
                onBack = viewModel::back,
                onNext = viewModel::next,
                onSave = {
                    if (state.testState is TestState.Idle) {
                        showUntestedDialog = true
                    } else {
                        viewModel.save()
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LinearProgressIndicator(
                progress = { (state.step + 1) / WIZARD_STEP_COUNT.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.wizard_step_label, state.step + 1, stepTitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                when (state.step) {
                    0 -> NameStep(state = state, onNameChange = viewModel::onNameChange)
                    1 -> TriggerStep(
                        selected = state.triggerKind,
                        onSelect = viewModel::onTriggerKindChange,
                    )
                    2 -> if (state.triggerKind == TriggerKind.NOTIFICATION) {
                        NotificationConfigStep(
                            state = state,
                            onToggleApp = viewModel::toggleApp,
                            onRemoveApp = viewModel::removeApp,
                            onKeywordInputChange = viewModel::onKeywordInputChange,
                            onAddKeyword = viewModel::addKeyword,
                            onRemoveKeyword = viewModel::removeKeyword,
                        )
                    } else {
                        IntervalConfigStep(
                            state = state,
                            onIntervalChange = viewModel::onIntervalChange,
                        )
                    }
                    3 -> WebhookStep(
                        state = state,
                        onUrlChange = viewModel::onUrlChange,
                        onMethodChange = viewModel::onMethodChange,
                        onHeaderChange = viewModel::onHeaderChange,
                        onAddHeader = viewModel::addHeader,
                        onRemoveHeader = viewModel::removeHeader,
                        onBodyChange = viewModel::onBodyChange,
                        onInsertPlaceholder = viewModel::insertPlaceholder,
                    )
                    else -> TestStep(
                        state = state,
                        onTest = viewModel::test,
                    )
                }
                if (state.saveError) {
                    Text(
                        text = stringResource(R.string.wizard_save_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (showUntestedDialog) {
        AlertDialog(
            onDismissRequest = { showUntestedDialog = false },
            title = { Text(stringResource(R.string.wizard_save_confirm_title)) },
            text = { Text(stringResource(R.string.wizard_save_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUntestedDialog = false
                        viewModel.save()
                    },
                ) { Text(stringResource(R.string.wizard_save_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showUntestedDialog = false }) {
                    Text(stringResource(R.string.wizard_save_confirm_no))
                }
            },
        )
    }
}

@Composable
private fun WizardBottomBar(
    step: Int,
    canNext: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
) {
    val isLastStep = step == WIZARD_STEP_COUNT - 1
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_back))
                }
            }
            QRISHookButton(
                text = stringResource(if (isLastStep) R.string.action_save_enable else R.string.action_next),
                onClick = if (isLastStep) onSave else onNext,
                enabled = canNext,
                loading = saving,
                modifier = Modifier.weight(if (step > 0) 2f else 1f),
            )
        }
    }
}

@Composable
private fun NameStep(state: WizardUiState, onNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.wizard_name_question),
            style = MaterialTheme.typography.headlineSmall,
        )
        QRISHookTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.wizard_name_label),
            helperText = stringResource(R.string.wizard_name_counter, state.name.length),
        )
    }
}

@Composable
private fun TriggerStep(selected: TriggerKind, onSelect: (TriggerKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        TriggerOptionCard(
            type = TriggerKind.NOTIFICATION,
            selected = selected == TriggerKind.NOTIFICATION,
            onClick = { onSelect(TriggerKind.NOTIFICATION) },
        )
        TriggerOptionCard(
            type = TriggerKind.INTERVAL,
            selected = selected == TriggerKind.INTERVAL,
            onClick = { onSelect(TriggerKind.INTERVAL) },
        )
    }
}

@Composable
private fun NotificationConfigStep(
    state: WizardUiState,
    onToggleApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onKeywordInputChange: (String) -> Unit,
    onAddKeyword: () -> Unit,
    onRemoveKeyword: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(stringResource(R.string.wizard_apps_section))

        if (state.sourceApps.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                state.sourceApps.forEach { packageName ->
                    val label = state.installedApps
                        .firstOrNull { it.packageName == packageName }?.label ?: packageName
                    InputChip(
                        selected = true,
                        onClick = { onRemoveApp(packageName) },
                        label = { Text(label) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_remove_app),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.wizard_apps_helper),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.loadingApps) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Column {
                state.installedApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app.packageName) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = app.packageName in state.sourceApps,
                            onCheckedChange = { onToggleApp(app.packageName) },
                        )
                        Column {
                            Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.wizard_keywords_section))
        QRISHookTextField(
            value = state.keywordInput,
            onValueChange = onKeywordInputChange,
            label = stringResource(R.string.wizard_keyword_label),
            trailingIcon = {
                IconButton(onClick = onAddKeyword) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_keyword),
                    )
                }
            },
        )
        if (state.keywords.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                state.keywords.forEach { keyword ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveKeyword(keyword) },
                        label = { Text(keyword) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_remove_keyword),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.wizard_keywords_helper),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IntervalConfigStep(state: WizardUiState, onIntervalChange: (String) -> Unit) {
    val labels = stringArrayResource(R.array.wizard_interval_quick_labels)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(stringResource(R.string.wizard_interval_section))
        QRISHookTextField(
            value = state.intervalInput,
            onValueChange = onIntervalChange,
            label = stringResource(R.string.wizard_interval_label),
            keyboardType = KeyboardType.Number,
            errorText = if (state.intervalTooShort) stringResource(R.string.wizard_interval_error) else null,
            helperText = stringResource(R.string.wizard_interval_helper),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            QUICK_INTERVALS.forEachIndexed { index, minutes ->
                FilterChip(
                    selected = state.intervalMinutes == minutes,
                    onClick = { onIntervalChange(minutes.toString()) },
                    label = { Text(labels.getOrElse(index) { minutes.toString() }) },
                )
            }
        }
    }
}

@Composable
private fun WebhookStep(
    state: WizardUiState,
    onUrlChange: (String) -> Unit,
    onMethodChange: (HttpMethod) -> Unit,
    onHeaderChange: (Int, String?, String?) -> Unit,
    onAddHeader: () -> Unit,
    onRemoveHeader: (Int) -> Unit,
    onBodyChange: (String) -> Unit,
    onInsertPlaceholder: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        QRISHookTextField(
            value = state.url,
            onValueChange = onUrlChange,
            label = stringResource(R.string.wizard_url_label),
            keyboardType = KeyboardType.Uri,
            errorText = if (state.url.isNotBlank() && !state.urlValid) {
                stringResource(R.string.wizard_url_error)
            } else {
                null
            },
            helperText = stringResource(R.string.wizard_url_helper),
        )

        SectionHeader(stringResource(R.string.wizard_method_section))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MethodOption("POST", state.method == HttpMethod.POST) { onMethodChange(HttpMethod.POST) }
            MethodOption("GET", state.method == HttpMethod.GET) { onMethodChange(HttpMethod.GET) }
        }

        SectionHeader(stringResource(R.string.wizard_headers_section))
        state.headers.forEachIndexed { index, header ->
            HeaderRow(
                key = header.key,
                value = header.value,
                onKeyChange = { onHeaderChange(index, it, null) },
                onValueChange = { onHeaderChange(index, null, it) },
                onDelete = { onRemoveHeader(index) },
            )
        }
        TextButton(onClick = onAddHeader) {
            Text(stringResource(R.string.action_add_header))
        }

        SectionHeader(stringResource(R.string.wizard_body_label))
        QRISHookTextField(
            value = state.bodyTemplate,
            onValueChange = onBodyChange,
            label = stringResource(
                if (state.method == HttpMethod.GET) R.string.wizard_body_label_get else R.string.wizard_body_label,
            ),
            minLines = 6,
            maxLines = 12,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
        if (state.method == HttpMethod.GET) {
            Text(
                text = stringResource(R.string.wizard_body_helper),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(text = stringResource(R.string.wizard_placeholders), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            PLACEHOLDERS.forEach { placeholder ->
                PlaceholderChip(text = placeholder, onClick = { onInsertPlaceholder(placeholder) })
            }
        }
        Text(
            text = stringResource(R.string.wizard_placeholders_helper),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MethodOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TestStep(state: WizardUiState, onTest: () -> Unit) {
    val triggerLabel = stringResource(
        if (state.triggerKind == TriggerKind.NOTIFICATION) {
            R.string.trigger_notification
        } else {
            R.string.trigger_interval
        },
    )
    val payload = TemplateEngine.render(
        template = state.bodyTemplate,
        context = state.dummyContext(),
        escapeJson = true,
    )

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionHeader(stringResource(R.string.wizard_summary_section))
        QRISHookCard {
            KeyValueRow(stringResource(R.string.wizard_summary_name), state.name)
            KeyValueRow(stringResource(R.string.wizard_summary_trigger), triggerLabel)
            if (state.triggerKind == TriggerKind.NOTIFICATION) {
                KeyValueRow(
                    stringResource(R.string.wizard_summary_keywords),
                    state.keywords.joinToString(", ").ifEmpty {
                        stringResource(R.string.wizard_summary_none)
                    },
                )
            }
            KeyValueRow(
                stringResource(R.string.wizard_summary_webhook),
                "${state.method} ${state.url}",
            )
            KeyValueRow(
                stringResource(R.string.wizard_summary_headers),
                stringResource(R.string.wizard_summary_header_count, state.activeHeaders().size),
            )
        }

        OutlinedButton(
            onClick = onTest,
            enabled = state.testState != TestState.Loading && state.urlValid,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (state.testState == TestState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.wizard_test_now))
            }
        }

        TestResultCard(state.testState)

        SectionHeader(stringResource(R.string.wizard_payload_preview))
        QRISHookCard {
            SelectionContainer {
                Text(
                    text = payload,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

@Composable
private fun TestResultCard(state: TestState) {
    when (state) {
        TestState.Idle -> QRISHookCard {
            Text(
                text = stringResource(R.string.wizard_test_idle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TestState.Loading -> QRISHookCard {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        is TestState.Success -> Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "✓ " + stringResource(
                        R.string.wizard_test_success,
                        state.code,
                        state.durationMs,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.response.isNotBlank()) {
                    Text(
                        text = state.response.take(500),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
        is TestState.Failed -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "✗ " + if (state.code != null) {
                        stringResource(R.string.wizard_test_failed, state.code.toString())
                    } else {
                        stringResource(R.string.wizard_test_network)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                if (state.response.isNotBlank()) {
                    Text(
                        text = state.response.take(500),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NameStepPreview() {
    QRISHookTheme {
        NameStep(
            state = WizardUiState(name = "QRIS masuk", bodyTemplate = PayloadTemplates.TRANSACTION),
            onNameChange = {},
        )
    }
}
