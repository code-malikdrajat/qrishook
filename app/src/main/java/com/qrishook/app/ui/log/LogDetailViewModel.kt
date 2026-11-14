package com.qrishook.app.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.domain.execution.AutomationExecutor
import com.qrishook.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LogDetailUiState {
    data object Loading : LogDetailUiState
    data object NotFound : LogDetailUiState
    data class Content(val log: ExecutionLog, val resending: Boolean = false) : LogDetailUiState
}

@HiltViewModel
class LogDetailViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val automationRepository: AutomationRepository,
    private val executor: AutomationExecutor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val logId: Long = savedStateHandle[Routes.LOG_ARG_ID] ?: -1L

    private val _uiState = MutableStateFlow<LogDetailUiState>(LogDetailUiState.Loading)
    val uiState: StateFlow<LogDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun resend() {
        val current = _uiState.value as? LogDetailUiState.Content ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(resending = true)
            val automation = automationRepository.getById(current.log.automationId)
            if (automation != null) {
                executor.sendPayload(automation, current.log.payloadSent)
            }
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val log = logRepository.getById(logId)
            _uiState.value = if (log == null) {
                LogDetailUiState.NotFound
            } else {
                LogDetailUiState.Content(log)
            }
        }
    }
}
