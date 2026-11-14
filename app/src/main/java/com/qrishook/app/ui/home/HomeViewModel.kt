package com.qrishook.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrishook.app.R
import com.qrishook.app.data.model.Automation
import com.qrishook.app.data.permission.PermissionChecker
import com.qrishook.app.data.permission.PermissionState
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.service.QRISHookForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val automations: List<Automation>) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AutomationRepository,
    private val permissionChecker: PermissionChecker,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.getAll()
        .map { HomeUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private val _permissions = MutableStateFlow(permissionChecker.state())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        // Pastikan service hidup lagi saat app dibuka (mis. setelah proses dimatikan OEM).
        viewModelScope.launch {
            if (repository.getEnabled().isNotEmpty()) {
                runCatching { QRISHookForegroundService.start(context) }
            }
        }
    }

    fun refreshPermissions() {
        _permissions.value = permissionChecker.state()
    }

    fun openPermissionSettings() = permissionChecker.openNotificationAccessSettings()

    fun setEnabled(automation: Automation, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(automation.id, enabled)
            if (enabled) {
                runCatching { QRISHookForegroundService.start(context) }
            }
            _messages.send(if (enabled) R.string.home_toggle_on else R.string.home_toggle_off)
        }
    }
}
