package com.qrishook.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qrishook.app.ui.home.HomeScreen
import com.qrishook.app.ui.onboarding.OnboardingScreen

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
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.WIZARD_ARG_ID)?.takeIf { it > 0 }
            PlaceholderScreen(if (id == null) "Screen Wizard (Buat)" else "Screen Wizard (Edit $id)")
        }

        composable(
            route = Routes.AUTOMATION,
            arguments = listOf(navArgument(Routes.AUTOMATION_ARG_ID) { type = NavType.LongType }),
        ) { entry ->
            PlaceholderScreen("Screen Automation ${entry.arguments?.getLong(Routes.AUTOMATION_ARG_ID)}")
        }

        composable(
            route = Routes.LOGS,
            arguments = listOf(
                navArgument(Routes.WIZARD_ARG_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong(Routes.WIZARD_ARG_ID)?.takeIf { it > 0 }
            PlaceholderScreen(if (id == null) "Screen Log List" else "Screen Log List (automation $id)")
        }

        composable(
            route = Routes.LOG,
            arguments = listOf(navArgument(Routes.LOG_ARG_ID) { type = NavType.LongType }),
        ) { entry ->
            PlaceholderScreen("Screen Log Detail ${entry.arguments?.getLong(Routes.LOG_ARG_ID)}")
        }

        composable(Routes.SETTINGS) { PlaceholderScreen("Screen Settings") }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}
