package com.qrishook.app.ui.onboarding

import androidx.lifecycle.ViewModel
import com.qrishook.app.data.permission.PermissionChecker
import com.qrishook.app.data.permission.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _permissions = MutableStateFlow(permissionChecker.state())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    fun refresh() {
        _permissions.value = permissionChecker.state()
    }

    fun openNotificationAccess() = permissionChecker.openNotificationAccessSettings()

    fun openNotificationPermission() = permissionChecker.openAppNotificationSettings()

    fun openBatteryOptimization() = permissionChecker.openBatteryOptimizationSettings()
}
