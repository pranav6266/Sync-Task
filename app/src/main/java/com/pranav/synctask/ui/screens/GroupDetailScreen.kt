package com.pranav.synctask.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.Space
import com.pranav.synctask.models.Task
import com.pranav.synctask.ui.components.AddTaskDialog
import com.pranav.synctask.ui.components.TaskItem
import com.pranav.synctask.ui.viewmodels.GroupDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    spaceId: String,
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    val spaceResult by viewModel.getSpace().observeAsState(Result.Loading())
    val membersResult by viewModel.getMembers().observeAsState(Result.Loading())
    val tasksResult by viewModel.getTasks().observeAsState(Result.Loading())
    val leaveResult by viewModel.getLeaveSpaceResult().observeAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId) {
        viewModel.loadSpaceData(spaceId)
    }

    LaunchedEffect(leaveResult) {
        if (leaveResult is Result.Success) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (spaceResult is Result.Success) {
                        Text((spaceResult as Result.Success<Space>).data.spaceName ?: "Group")
                    } else {
                        Text("Loading...")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Copy Invite Code") },
                            onClick = {
                                if (spaceResult is Result.Success) {
                                    val code = (spaceResult as Result.Success<Space>).data.inviteCode
                                    clipboardManager.setText(AnnotatedString(code ?: ""))
                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave Group", color = Color.Red) },
                            onClick = {
                                viewModel.leaveGroup(spaceId)
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Default.Add, "Add Task")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // Members Strip
            if (membersResult is Result.Success) {
                val members = (membersResult as Result.Success).data
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy((-8).dp) // Overlap effect
                ) {
                    members.forEach { user ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName?.firstOrNull()?.toString() ?: "U",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (members.isEmpty()) Text("No members", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Task List
            Box(modifier = Modifier.weight(1f)) {
                when (val result = tasksResult) {
                    is Result.Loading<*> -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is Result.Success<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val tasks = result.data as? List<Task> ?: emptyList()

                        if (tasks.isEmpty()) {
                            Text("No tasks yet.", Modifier.align(Alignment.Center), color = Color.Gray)
                        } else {
                            LazyColumn {
                                items(tasks) { task ->
                                    TaskItem(
                                        task = task,
                                        onCheckedChange = { isChecked -> viewModel.updateTaskStatus(task.id, isChecked) },
                                        onTaskClick = { onTaskClick(task.id) }
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { title ->
                    if (currentUser != null) {
                        val task = Task()
                        task.title = title
                        task.spaceId = spaceId // Important!
                        task.creatorUID = currentUser.uid
                        task.status = Task.STATUS_PENDING
                        task.ownershipScope = Task.SCOPE_SHARED

                        viewModel.createTask(task)
                    }
                    showAddTaskDialog = false
                }
            )
        }
    }
}