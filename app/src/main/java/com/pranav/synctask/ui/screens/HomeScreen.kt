package com.pranav.synctask.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Keep this import!
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState // This should now work
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.Task
import com.pranav.synctask.ui.components.EmptyState
import com.pranav.synctask.ui.components.TaskItem
import com.pranav.synctask.ui.viewmodels.TasksViewModel

@Composable
fun HomeScreen(
    onTaskClick: (String) -> Unit,
    viewModel: TasksViewModel = viewModel()
) {

    val currentUser = FirebaseAuth.getInstance().currentUser
    // FIX 1: Explicitly tell observeAsState the type to help inference
    val tasksResult by viewModel.personalTasks.observeAsState(initial = Result.Loading())

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            viewModel.loadPersonalTasks(uid)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        when (val result = tasksResult) {
            // FIX 2: Use <*>. This tells Kotlin "It's a Loading/Error/Success of ANY type"
            is Result.Loading<*> -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is Result.Error<*> -> {
                Text(
                    text = "Error: ${result.exception?.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is Result.Success<*> -> {
                @Suppress("UNCHECKED_CAST")
                val tasks = result.data as? List<Task> ?: emptyList() // <--- 'tasks' is defined here

                if (tasks.isEmpty()) {
                    // --- DELETE THE OLD TEXT COMPOSABLE ---
                    /* Text(
                        text = "No personal tasks yet!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    */

                    // --- ADD THE NEW ANIMATION HERE ---
                    EmptyState(message = "All caught up! No personal tasks.")

                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(tasks) { task ->
                            TaskItem(
                                task = task,
                                onCheckedChange = { isChecked ->
                                    val newStatus = if (isChecked) Task.STATUS_COMPLETED else Task.STATUS_PENDING
                                    viewModel.updateTaskStatus(task.id, newStatus)
                                },
                                onTaskClick = { onTaskClick(task.id) }
                            )
                        }
                    }
                }
            }
            // Add else branch because 'val result' type might be inferred loosely
            else -> {}
        }
    }


}