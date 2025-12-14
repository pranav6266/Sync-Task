package com.pranav.synctask.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.Task
import com.pranav.synctask.ui.viewmodels.EditTaskViewModel
import com.pranav.synctask.utils.DateUtils
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: String,
    onBack: () -> Unit,
    viewModel: EditTaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val updateResult by viewModel.updateTask(Task()).observeAsState() // Placeholder observation

    // We need to load the task first.
    // Since EditTaskViewModel usually expects a Task object, we might need a loader.
    // For simplicity, we can fetch the task via the Repository directly here or add a load method.
    // Let's assume we pass the Task ID and the VM fetches it.

    // State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var dueDate by remember { mutableStateOf<Date?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var taskToUpdate by remember { mutableStateOf<Task?>(null) }

    // Load Task Data
    LaunchedEffect(taskId) {
        // You might need to add getTask(taskId) to EditTaskViewModel or use TaskDetailViewModel
        // Here we can use the repository singleton pattern for a quick fetch
        com.pranav.synctask.data.TaskRepository.getInstance().attachTaskListener(taskId)
    }

    val taskLoadResult by com.pranav.synctask.data.TaskRepository.getInstance().taskById.observeAsState()

    LaunchedEffect(taskLoadResult) {
        if (taskLoadResult is Result.Success) {
            val task = (taskLoadResult as Result.Success<Task>).data
            if (task != null && taskToUpdate == null) { // Only set once
                taskToUpdate = task
                title = task.title ?: ""
                description = task.description ?: ""
                priority = task.priority ?: "Normal"
                dueDate = task.dueDateAsDate
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (taskToUpdate != null && title.isNotBlank()) {
                                taskToUpdate!!.title = title
                                taskToUpdate!!.description = description
                                taskToUpdate!!.priority = priority
                                taskToUpdate!!.setDueDateFromDate(dueDate)

                                viewModel.updateTask(taskToUpdate!!).observeForever { result ->
                                    if (result is Result.Success) {
                                        Toast.makeText(context, "Task Updated", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Priority Dropdown (Simplified as Row)
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Normal", "Low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                // Date Picker
                OutlinedTextField(
                    value = if (dueDate != null) DateUtils.formatDate(Timestamp(dueDate!!)) else "",
                    onValueChange = { },
                    label = { Text("Due Date") },
                    modifier = Modifier.fillMaxWidth().clickable { /* Open Date Picker */ },
                    enabled = false, // Disable typing, handle click
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            if (dueDate != null) calendar.time = dueDate!!
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    calendar.set(y, m, d)
                                    dueDate = calendar.time
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, "Select Date")
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}