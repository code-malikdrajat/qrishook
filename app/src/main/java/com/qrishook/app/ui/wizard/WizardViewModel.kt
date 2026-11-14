package com.qrishook.app.ui.wizard

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrishook.app.data.model.Action
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.prefs.SecurePrefs
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.domain.execution.PayloadTemplates
import com.qrishook.app.domain.webhook.TemplateEngine
import com.qrishook.app.domain.webhook.WebhookClient
import com.qrishook.app.domain.webhook.WebhookResult
import com.qrishook.app.navigation.Routes
import com.qrishook.app.service.IntervalWorker
import com.qrishook.app.service.QRISHookForegroundService
import com.qrishook.app.ui.components.TriggerKind
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val repository: AutomationRepository,
    private val securePrefs: SecurePrefs,
    private val webhookClient: WebhookClient,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(WizardUiState())
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    init {
        loadInstalledApps()
        savedStateHandle.get<Long>(Routes.WIZARD_ARG_ID)
            ?.takeIf { it > 0 }
            ?.let(::loadAutomation)
    }

    fun next() {
        _state.update { if (it.step < WIZARD_STEP_COUNT - 1 && it.canGoNext) it.copy(step = it.step + 1) else it }
    }

    fun back() {
        _state.update { if (it.step > 0) it.copy(step = it.step - 1) else it }
    }

    fun onNameChange(value: String) {
        if (value.length <= NAME_MAX_LENGTH) _state.update { it.copy(name = value) }
    }

    fun onTriggerKindChange(kind: TriggerKind) {
        _state.update { current ->
            if (current.triggerKind == kind) {
                current
            } else {
                val trigger = when (kind) {
                    TriggerKind.NOTIFICATION -> Trigger.NotificationTrigger()
                    TriggerKind.INTERVAL -> Trigger.IntervalTrigger(
                        (current.intervalMinutes ?: Trigger.IntervalTrigger.MIN_INTERVAL_MINUTES)
                            .coerceAtLeast(Trigger.IntervalTrigger.MIN_INTERVAL_MINUTES),
                    )
                }
                current.copy(
                    triggerKind = kind,
                    bodyTemplate = PayloadTemplates.defaultFor(trigger),
                    testState = TestState.Idle,
                )
            }
        }
    }

    fun toggleApp(packageName: String) {
        _state.update { current ->
            val apps = if (packageName in current.sourceApps) {
                current.sourceApps - packageName
            } else {
                current.sourceApps + packageName
            }
            current.copy(sourceApps = apps)
        }
    }

    fun onKeywordInputChange(value: String) {
        _state.update { it.copy(keywordInput = value) }
    }

    fun addKeyword() {
        _state.update { current ->
            val keyword = current.keywordInput.trim()
            val duplicate = current.keywords.any { it.equals(keyword, ignoreCase = true) }
            if (keyword.isEmpty() || duplicate) {
                current.copy(keywordInput = "")
            } else {
                current.copy(keywords = current.keywords + keyword, keywordInput = "")
            }
        }
    }

    fun removeKeyword(keyword: String) {
        _state.update { it.copy(keywords = it.keywords - keyword) }
    }

    fun removeApp(packageName: String) {
        _state.update { it.copy(sourceApps = it.sourceApps - packageName) }
    }

    fun onIntervalChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 5) {
            _state.update { it.copy(intervalInput = value) }
        }
    }

    fun onUrlChange(value: String) {
        _state.update { it.copy(url = value, testState = TestState.Idle) }
    }

    fun onMethodChange(method: HttpMethod) {
        _state.update { it.copy(method = method, testState = TestState.Idle) }
    }

    fun onHeaderChange(index: Int, key: String? = null, value: String? = null) {
        _state.update { current ->
            current.copy(
                headers = current.headers.mapIndexed { i, header ->
                    when {
                        i != index -> header
                        key != null -> header.copy(key = key)
                        value != null -> header.copy(value = value)
                        else -> header
                    }
                },
            )
        }
    }

    fun addHeader() {
        _state.update { it.copy(headers = it.headers + KeyValue()) }
    }

    fun removeHeader(index: Int) {
        _state.update { it.copy(headers = it.headers.filterIndexed { i, _ -> i != index }) }
    }

    fun onBodyChange(value: String) {
        _state.update { it.copy(bodyTemplate = value) }
    }

    fun insertPlaceholder(placeholder: String) {
        _state.update { it.copy(bodyTemplate = it.bodyTemplate + placeholder) }
    }

    fun test() {
        val current = _state.value
        if (current.url.toHttpUrlOrNull() == null) return

        viewModelScope.launch {
            _state.update { it.copy(testState = TestState.Loading) }
            val payload = TemplateEngine.render(
                template = current.bodyTemplate,
                context = current.dummyContext(),
                escapeJson = true,
            )
            val result = runCatching {
                webhookClient.send(
                    url = current.url,
                    method = current.method,
                    headers = current.activeHeaders(),
                    body = payload,
                )
            }.getOrElse { WebhookResult.NetworkError(it.message ?: "Gagal mengirim") }
            _state.update {
                it.copy(
                    testState = when (result) {
                        is WebhookResult.Success -> TestState.Success(
                            code = result.httpCode,
                            durationMs = result.durationMs,
                            response = result.body,
                        )
                        is WebhookResult.Failed -> TestState.Failed(
                            code = result.httpCode,
                            message = result.error ?: "HTTP ${result.httpCode}",
                            response = result.body,
                        )
                        is WebhookResult.NetworkError -> TestState.Failed(
                            code = null,
                            message = result.message,
                            response = "",
                        )
                    },
                )
            }
        }
    }

    fun save() {
        val current = _state.value
        if (!current.canGoNext || current.saving) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = false) }
            runCatching {
                val trigger = current.toTrigger()
                // URL & headers hanya disimpan di EncryptedSharedPreferences (PRD NFR).
                val storedAction = Action(
                    url = "",
                    method = current.method,
                    headers = emptyList(),
                    bodyTemplate = current.bodyTemplate,
                )
                val existing = current.automationId?.let { repository.getById(it) }
                val automation = existing?.copy(
                    name = current.name.trim(),
                    trigger = trigger,
                    action = storedAction,
                ) ?: Automation(
                    name = current.name.trim(),
                    trigger = trigger,
                    action = storedAction,
                )

                val id = if (existing == null) {
                    repository.insert(automation)
                } else {
                    repository.update(automation)
                    automation.id
                }
                securePrefs.saveWebhookSecret(id, current.url.trim(), current.activeHeaders())

                val enabled = automation.enabled
                if (enabled) {
                    if (trigger is Trigger.IntervalTrigger) {
                        IntervalWorker.schedule(context, automation.copy(id = id))
                    }
                    runCatching { QRISHookForegroundService.start(context) }
                }
            }.onSuccess {
                _state.update { it.copy(saving = false, saved = true) }
            }.onFailure {
                _state.update { it.copy(saving = false, saveError = true) }
            }
        }
    }

    private fun loadAutomation(id: Long) {
        viewModelScope.launch {
            val automation = repository.getById(id) ?: return@launch
            val secret = securePrefs.getWebhookSecret(id)
            val trigger = automation.trigger
            _state.update {
                it.copy(
                    automationId = id,
                    isEdit = true,
                    name = automation.name,
                    triggerKind = when (trigger) {
                        is Trigger.NotificationTrigger -> TriggerKind.NOTIFICATION
                        is Trigger.IntervalTrigger -> TriggerKind.INTERVAL
                    },
                    sourceApps = (trigger as? Trigger.NotificationTrigger)?.sourceApps.orEmpty(),
                    keywords = (trigger as? Trigger.NotificationTrigger)?.keywords.orEmpty(),
                    intervalInput = (trigger as? Trigger.IntervalTrigger)?.intervalMinutes?.toString() ?: "15",
                    url = secret?.url ?: automation.action.url,
                    method = automation.action.method,
                    headers = secret?.headers ?: automation.action.headers,
                    bodyTemplate = automation.action.bodyTemplate.ifBlank {
                        PayloadTemplates.defaultFor(trigger)
                    },
                )
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                runCatching {
                    val pm = context.packageManager
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    pm.queryIntentActivities(intent, 0)
                        .mapNotNull { it.activityInfo?.packageName }
                        .distinct()
                        .map { packageName ->
                            val label = runCatching {
                                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                            }.getOrDefault(packageName)
                            InstalledApp(packageName, label)
                        }
                        .sortedBy { it.label.lowercase() }
                }.getOrDefault(emptyList())
            }
            _state.update { it.copy(installedApps = apps, loadingApps = false) }
        }
    }
}
