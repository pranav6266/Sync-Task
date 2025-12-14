package com.pranav.synctask.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pranav.synctask.ui.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    onLoginClick: () -> Unit,
    deepLinkData: Pair<String?, String>? = null
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") { LoginScreen(onLoginClick = onLoginClick) }

        composable("main") {
            MainScreen(
                onNavigateToTask = { taskId -> navController.navigate("task_detail/$taskId") },
                onNavigateToGroup = { spaceId -> navController.navigate("group_detail/$spaceId") }
            )
        }

        composable(
            route = "task_detail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            if (taskId != null) {
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() },
                    onEditClick = { navController.navigate("edit_task/$taskId") }
                )
            }
        }

        // NEW: Edit Task Route
        composable(
            route = "edit_task/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            if (taskId != null) {
                EditTaskScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // NEW: Group Detail Route
        composable(
            route = "group_detail/{spaceId}",
            arguments = listOf(navArgument("spaceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceId = backStackEntry.arguments?.getString("spaceId")
            if (spaceId != null) {
                GroupDetailScreen(
                    spaceId = spaceId,
                    onBack = { navController.popBackStack() },
                    onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") }
                )
            }
        }
    }
    // ADD THIS BLOCK AFTER NavHost definition or inside a LaunchedEffect
    LaunchedEffect(deepLinkData) {
        if (deepLinkData != null) {
            val (type, id) = deepLinkData
            if (type == "CHAT" && id != null) {
                // Since "Partner" is a fixed tab, we just navigate there.
                // Note: For generic groups, you'd use "group_detail/$id"
                navController.navigate("partner")
            } else if (type == "TASK" && id != null) {
                navController.navigate("task_detail/$id")
            }
        }
    }
}