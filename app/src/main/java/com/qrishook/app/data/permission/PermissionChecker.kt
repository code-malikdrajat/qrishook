package com.qrishook.app.data.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.qrishook.app.service.NotifListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionState(
    val notificationAccess: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val allGranted: Boolean get() = notificationAccess && notifications && batteryUnrestricted
}

@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun state(): PermissionState = PermissionState(
        notificationAccess = NotifListenerService.isPermissionGranted(context),
        notifications = hasNotificationPermission(),
        batteryUnrestricted = isIgnoringBatteryOptimizations(),
    )

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openNotificationAccessSettings() {
        runCatching { launchSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    fun openAppNotificationSettings() {
        runCatching {
            launchSettings(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
    }

    fun openBatteryOptimizationSettings() {
        val request = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        runCatching { launchSettings(request) }.onFailure {
            launchSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun launchSettings(intent: Intent) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
