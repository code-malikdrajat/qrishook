package com.qrishook.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.qrishook.app.data.permission.PermissionChecker
import com.qrishook.app.data.permission.PermissionState
import com.qrishook.app.service.QRISHookForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val permissions: PermissionState,
    val serviceRunning: Boolean,
    val version: String,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = loadState()
    }

    fun openNotificationAccess() = permissionChecker.openNotificationAccessSettings()

    fun openNotificationPermission() = permissionChecker.openAppNotificationSettings()

    fun openBatteryOptimization() = permissionChecker.openBatteryOptimizationSettings()

    fun openOemGuide() = openBrowser(OEM_GUIDE_URL)

    fun openSourceCode() = openBrowser(SOURCE_CODE_URL)

    private fun loadState() = SettingsUiState(
        permissions = permissionChecker.state(),
        serviceRunning = QRISHookForegroundService.isRunning,
        version = appVersion(),
    )

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    private fun openBrowser(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    companion object {
        private const val OEM_GUIDE_URL = "https://dontkillmyapp.com"
        private const val SOURCE_CODE_URL = "https://github.com/qrishook/qrishook"
    }
}
