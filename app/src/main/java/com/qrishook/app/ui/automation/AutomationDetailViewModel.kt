package com.qrishook.app.ui.automation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.model.Trigger
import com.qrishook.app.data.prefs.SecurePrefs
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.navigation.Routes
import com.qrishook.app.service.IntervalWorker
import com.qrishook.app.service.QRISHookForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    data class Content(val automation: Automation, val stats: DetailStats) : DetailUiState
}

@HiltViewModel
class AutomationDetailViewModel @Inject constructor(
    private val repository: AutomationRepository,
    private val logRepository: LogRepository,
    private val securePrefs: SecurePrefs,
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
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val automation = currentAutomation() ?: return@launch
            repository.setEnabled(automation.id, enabled)
            if (enabled) {
                QRISHookForegroundService.start(context)
                if (automation.trigger is Trigger.IntervalTrigger) {
                    IntervalWorker.schedule(context, automation.copy(enabled = true))
                }
            } else if (automation.trigger is Trigger.IntervalTrigger) {
                IntervalWorker.cancel(context, automation.id)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            val automation = currentAutomation() ?: return@launch
            IntervalWorker.cancel(context, automation.id)
            securePrefs.deleteWebhookSecret(automation.id)
            repository.delete(automation)
            _deleted.value = true
        }
    }

    private fun currentAutomation(): Automation? =
        (uiState.value as? DetailUiState.Content)?.automation
}
