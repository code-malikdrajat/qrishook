package com.qrishook.app.ui.automation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrishook.app.R
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.prefs.SecurePrefs
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.domain.execution.AutomationExecutor
import com.qrishook.app.domain.execution.testContext
import com.qrishook.app.domain.webhook.WebhookResult
import com.qrishook.app.navigation.Routes
import com.qrishook.app.service.QRISHookForegroundService
import com.qrishook.app.util.Battery
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailStats(
    val total: Int = 0,
    val success: Int = 0,
    val failed: Int = 0,
)

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Content(
        val automation: Automation,
        val stats: DetailStats,
        val webhookUrl: String,
    ) : DetailUiState
}

sealed interface DetailEvent {
    data class Tested(val success: Boolean, val code: Int?, val durationMs: Long?) : DetailEvent
    data class Duplicated(val id: Long) : DetailEvent
}

@HiltViewModel
class AutomationDetailViewModel @Inject constructor(
    private val repository: AutomationRepository,
    private val logRepository: LogRepository,
    private val securePrefs: SecurePrefs,
    private val executor: AutomationExecutor,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val automationId: Long = savedStateHandle[Routes.AUTOMATION_ARG_ID] ?: -1L

    val uiState: StateFlow<DetailUiState> = combine(
        repository.getAll(),
        logRepository.getByAutomation(automationId),
    ) { automations, logs ->
        val automation = automations.firstOrNull { it.id == automationId }
        if (automation == null) {
            DetailUiState.NotFound
        } else {
            DetailUiState.Content(
                automation = automation,
                stats = DetailStats(
                    total = logs.size,
                    success = logs.count { it.status == LogStatus.SUCCESS },
                    failed = logs.count { it.status == LogStatus.FAILED },
                ),
                webhookUrl = securePrefs.getWebhookSecret(automationId)?.url
                    ?: automation.action.url,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val automation = currentAutomation() ?: return@launch
            repository.setEnabled(automation.id, enabled)
            if (enabled) {
                runCatching { QRISHookForegroundService.start(context) }
            }
        }
    }

    /** Kirim payload dummy; hasilnya tercatat sebagai log seperti eksekusi biasa. */
    fun test() {
        val automation = currentAutomation() ?: return
        if (_testing.value) return
        viewModelScope.launch {
            _testing.value = true
            val result = executor.execute(automation, testContext(Battery.percent(context)))
            _testing.value = false
            _events.send(
                DetailEvent.Tested(
                    success = result is WebhookResult.Success,
                    code = when (result) {
                        is WebhookResult.Success -> result.httpCode
                        is WebhookResult.Failed -> result.httpCode
                        is WebhookResult.NetworkError -> null
                    },
                    durationMs = (result as? WebhookResult.Success)?.durationMs,
                ),
            )
        }
    }

    fun duplicate() {
        viewModelScope.launch {
            val original = currentAutomation() ?: return@launch
            val copy = original.copy(
                id = 0,
                name = original.name + context.getString(R.string.detail_copy_suffix),
                enabled = false,
                createdAt = System.currentTimeMillis(),
                lastRunAt = null,
                lastStatus = null,
            )
            val newId = repository.insert(copy)
            securePrefs.getWebhookSecret(original.id)?.let { secret ->
                securePrefs.saveWebhookSecret(newId, secret.url, secret.headers)
            }
            _events.send(DetailEvent.Duplicated(newId))
        }
    }

    fun delete() {
        viewModelScope.launch {
            val automation = currentAutomation() ?: return@launch
            securePrefs.deleteWebhookSecret(automation.id)
            repository.delete(automation)
            _deleted.value = true
        }
    }

    private fun currentAutomation(): Automation? =
        (uiState.value as? DetailUiState.Content)?.automation
}
