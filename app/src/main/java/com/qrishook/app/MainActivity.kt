package com.qrishook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qrishook.app.data.permission.PermissionChecker
import com.qrishook.app.data.prefs.AppPrefs
import com.qrishook.app.navigation.QRISHookNavGraph
import com.qrishook.app.navigation.Routes
import com.qrishook.app.ui.theme.QRISHookTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPrefs: AppPrefs
    @Inject lateinit var permissionChecker: PermissionChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (appPrefs.onboardingDone || permissionChecker.state().allGranted) {
            Routes.HOME
        } else {
            Routes.ONBOARDING
        }

        setContent {
            QRISHookTheme {
                QRISHookNavGraph(startDestination = startDestination)
            }
        }
    }
}
