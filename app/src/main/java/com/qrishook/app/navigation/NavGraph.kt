package com.qrishook.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qrishook.app.ui.automation.AutomationDetailScreen
import com.qrishook.app.ui.home.HomeScreen
import com.qrishook.app.ui.log.LogDetailScreen
import com.qrishook.app.ui.log.LogListScreen
import com.qrishook.app.ui.onboarding.OnboardingScreen
import com.qrishook.app.ui.settings.SettingsScreen
import com.qrishook.app.ui.wizard.CreateAutomationScreen

@Composable
fun QRISHookNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onStart = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onCreate = { navController.navigate(Routes.wizard()) },
                onOpenAutomation = { navController.navigate(Routes.automation(it)) },
                onOpenLogs = { navController.navigate(Routes.logs()) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.WIZARD,
            arguments = listOf(
                navArgument(Routes.WIZARD_ARG_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            CreateAutomationScreen(
                onBackToHome = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(
            route = Routes.AUTOMATION,
            arguments = listOf(navArgument(Routes.AUTOMATION_ARG_ID) { type = NavType.LongType }),
        ) {
            AutomationDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.wizard(it)) },
                onOpenLogs = { navController.navigate(Routes.logs(it)) },
            )
        }

        composable(
            route = Routes.LOGS,
            arguments = listOf(
                navArgument(Routes.WIZARD_ARG_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            LogListScreen(
                onBack = { navController.popBackStack() },
                onOpenLog = { navController.navigate(Routes.log(it)) },
            )
        }

        composable(
            route = Routes.LOG,
            arguments = listOf(navArgument(Routes.LOG_ARG_ID) { type = NavType.LongType }),
        ) {
            LogDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
