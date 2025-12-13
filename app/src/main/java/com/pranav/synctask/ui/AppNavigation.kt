package com.pranav.synctask.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pranav.synctask.ui.screens.LoginScreen
import com.pranav.synctask.ui.screens.MainScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    onLoginClick: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // 1. Login Screen Route
        composable("login") {
            LoginScreen(onLoginClick = onLoginClick)
        }

        // 2. Main App Route
        composable("main") {
            MainScreen()
        }
    }
}