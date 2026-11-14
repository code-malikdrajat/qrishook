package com.qrishook.app.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrishook.app.data.model.ExecutionLog
import com.qrishook.app.data.model.LogStatus
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import com.qrishook.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LogFilter(val status: LogStatus?) {
    ALL(null),
    SUCCESS(LogStatus.SUCCESS),
    FAILED(LogStatus.FAILED),
    QUEUED(LogStatus.QUEUED),
}

data class LogListUiState(
    val loading: Boolean = true,
    val filter: LogFilter = LogFilter.ALL,
    val logs: List<ExecutionLog> = emptyList(),
    val automationName: String? = null,
)

@HiltViewModel
class LogListViewModel @Inject constructor(
    logRepository: LogRepository,
    automationRepository: AutomationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val automationId: Long? = savedStateHandle
        .get<Long>(Routes.WIZARD_ARG_ID)
        ?.takeIf { it > 0 }

    private val _filter = MutableStateFlow(LogFilter.ALL)
    private val logs = automationId
        ?.let { logRepository.getByAutomation(it) }
        ?: logRepository.getAll()

    val uiState: StateFlow<LogListUiState> = combine(logs, _filter) { allLogs, filter ->
        LogListUiState(
            loading = false,
            filter = filter,
            logs = allLogs.filter { filter.status == null || it.status == filter.status },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogListUiState())

    private val _automationName = MutableStateFlow<String?>(null)
    val automationName: StateFlow<String?> = _automationName

    init {
        val id = automationId ?: return@init
        viewModelScope.launch {
            _automationName.value = automationRepository.getById(id)?.name
        }
    }

    fun setFilter(filter: LogFilter) {
        _filter.value = filter
    }
}
