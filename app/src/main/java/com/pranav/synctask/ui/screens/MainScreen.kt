package com.pranav.synctask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // Helper to see which tab is active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Tab 1: Home
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Me") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                // Tab 2: Partner
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Partner") },
                    label = { Text("Us") },
                    selected = currentRoute == "partner",
                    onClick = { navController.navigate("partner") }
                )
                // Tab 3: Groups (Using Person icon as placeholder for Groups)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Groups") },
                    label = { Text("Groups") },
                    selected = currentRoute == "groups",
                    onClick = { navController.navigate("groups") }
                )
                // Tab 4: Settings
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { innerPadding ->
        // Placeholder Screens
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { PlaceholderScreen("My Tasks (Home)") }
            composable("partner") { PlaceholderScreen("Partner Chat & Tasks") }
            composable("groups") { PlaceholderScreen("Groups List") }
            composable("settings") { PlaceholderScreen("Settings") }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
    }
}