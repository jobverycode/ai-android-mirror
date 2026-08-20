package com.ai.mirror.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ai.mirror.ui.home.HomeScreen
import com.ai.mirror.ui.receiver.ReceiverScreen
import com.ai.mirror.ui.sender.SenderScreen
import com.ai.mirror.ui.settings.SettingsScreen

object NavRoutes {
    const val HOME = "home"
    const val SENDER = "sender"
    const val RECEIVER = "receiver?ip={ip}&port={port}"
    const val SETTINGS = "settings"

    fun receiverRoute(ip: String? = null, port: Int? = null): String {
        return if (!ip.isNullOrBlank() && port != null) {
            "receiver?ip=$ip&port=$port"
        } else {
            "receiver"
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToSender = {
                    navController.navigate(NavRoutes.SENDER)
                },
                onNavigateToReceiver = { ip, port ->
                    navController.navigate(NavRoutes.receiverRoute(ip, port))
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        composable(NavRoutes.SENDER) {
            SenderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.RECEIVER,
            arguments = listOf(
                navArgument("ip") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("port") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip")
            val port = backStackEntry.arguments?.getInt("port")?.takeIf { it > 0 }

            ReceiverScreen(
                targetIp = ip,
                targetPort = port,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
