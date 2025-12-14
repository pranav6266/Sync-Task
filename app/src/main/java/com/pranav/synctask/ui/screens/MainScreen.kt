package com.pranav.synctask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.pranav.synctask.models.Task
import com.pranav.synctask.ui.components.AddTaskDialog
import com.pranav.synctask.ui.viewmodels.PartnerViewModel
import com.pranav.synctask.ui.viewmodels.TasksViewModel

@Composable
fun MainScreen(
    onNavigateToTask: (String) -> Unit ,
    onNavigateToGroup: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val context = LocalContext.current

    // ViewModels
    val tasksViewModel: TasksViewModel = viewModel()
    val partnerViewModel: PartnerViewModel = viewModel()

    // Dialog State
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (currentRoute == "home" || currentRoute == "partner") {
                FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Me") },
                    label = { Text("Me") },
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Us") },
                    label = { Text("Us") },
                    selected = currentRoute == "partner",
                    onClick = { navController.navigate("partner") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "Groups") },
                    label = { Text("Groups") },
                    selected = currentRoute == "groups",
                    onClick = { navController.navigate("groups") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { innerPadding ->

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { title ->
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        if (currentRoute == "partner") {
                            partnerViewModel.createSharedTask(title)
                        } else {
                            val newTask = Task()
                            newTask.title = title
                            newTask.creatorUID = currentUser.uid
                            newTask.status = Task.STATUS_PENDING
                            newTask.ownershipScope = Task.SCOPE_INDIVIDUAL
                            tasksViewModel.createTask(newTask, context)
                        }
                    }
                    showAddTaskDialog = false
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // PASS THE CALLBACK TO SCREENS
            composable("home") {
                HomeScreen(onTaskClick = onNavigateToTask)
            }
            composable("partner") {
                PartnerScreen(onTaskClick = onNavigateToTask)
            }
            composable("groups") {
                GroupsScreen(onGroupClick = onNavigateToGroup) // Pass it here
            }
            composable("settings") {
                SettingsScreen(onLogout = { /* Handle logout in parent if needed */ })
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.headlineMedium)
    }
}