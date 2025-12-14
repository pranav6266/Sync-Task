package com.pranav.synctask.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.Task
import com.pranav.synctask.models.Subtask
import com.pranav.synctask.ui.viewmodels.TaskDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    viewModel: TaskDetailViewModel = viewModel()
) {
    val taskResult by viewModel.task.observeAsState(Result.Loading())
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(taskId) {
        viewModel.attachTaskListener(taskId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) { // Add Edit Icon
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        viewModel.deleteTask(taskId)
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val result = taskResult) {
                is Result.Loading<*> -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is Result.Error<*> -> Text("Error loading task", modifier = Modifier.align(Alignment.Center))
                is Result.Success<*> -> {
                    val task = result.data as? Task
                    if (task != null) {
                        TaskDetailContent(task, viewModel, currentUser?.uid)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun TaskDetailContent(task: Task, viewModel: TaskDetailViewModel, currentUserId: String?) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Title
        Text(
            text = task.title ?: "Untitled",
            style = MaterialTheme.typography.headlineMedium,
            textDecoration = if (task.status == Task.STATUS_COMPLETED) TextDecoration.LineThrough else null
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.status == Task.STATUS_COMPLETED,
                onCheckedChange = { isChecked ->
                    val newStatus = if (isChecked) Task.STATUS_COMPLETED else Task.STATUS_PENDING
                    viewModel.updateTaskStatus(task.id, newStatus)
                }
            )
            Text(if (task.status == Task.STATUS_COMPLETED) "Completed" else "Mark as Complete")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Subtasks", style = MaterialTheme.typography.titleMedium)

        // Subtask List
        val subtasks = task.subtasks ?: emptyList()
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(subtasks) { subtask ->
                SubtaskRow(subtask, task.id, viewModel, currentUserId)
            }
        }
    }
}

@Composable
fun SubtaskRow(
    subtask: Subtask,
    taskId: String,
    viewModel: TaskDetailViewModel,
    userId: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { isChecked ->
                if (userId != null) {
                    viewModel.toggleSubtaskCompletion(taskId, subtask.id, isChecked, userId)
                }
            }
        )
        Text(
            text = subtask.title ?: "",
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
            color = if (subtask.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
        )
    }
}